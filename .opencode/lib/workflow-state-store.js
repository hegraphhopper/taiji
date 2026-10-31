/* global process */
import { existsSync, mkdirSync, readFileSync, renameSync, unlinkSync, writeFileSync } from "node:fs"
import { randomUUID } from "node:crypto"
import { basename, dirname, join, relative, resolve } from "node:path"
import { debugLog } from "./trellis-context.js"

const SKILLS = {
  "trellis-brainstorm": ["plan", "use_case_design"],
  "trellis-grill-me": ["plan", "requirements"],
  "trellis-use-case-design": ["plan", "use_case_design"],
  "trellis-before-dev": ["execute", "context_loading"],
  "trellis-break-loop": ["finish", "repair"],
  "trellis-check": ["finish", "quality_check"],
  "trellis-unit-test": ["finish", "testing"],
  "trellis-api-test": ["finish", "testing"],
  "trellis-update-spec": ["finish", "spec_update"],
  "trellis-test-rework": ["finish", "repair"],
}

const ROLES = {
  "trellis-research": ["plan", "research"],
  "trellis-implement": ["execute", "implementation"],
  "trellis-check": ["finish", "quality_check"],
  "trellis-unit-test": ["finish", "testing"],
  "trellis-api-test": ["finish", "testing"],
  "trellis-test-rework": ["finish", "repair"],
}

const EDIT_TOOLS = new Set(["write", "edit", "apply_patch"])
const SOURCES = new Set(["workflow_status", "skill", "subagent", "tool"])
const PHASES = new Set(["plan", "execute", "finish"])
const STEPS = new Set([
  "task_planning",
  "requirements",
  "use_case_design",
  "research",
  "context_loading",
  "implementation",
  "rework",
  "quality_check",
  "testing",
  "test_fix",
  "repair",
  "spec_update",
])

function safeContextId(value) {
  return (
    typeof value === "string" &&
    value.length <= 128 &&
    /^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(value) &&
    value !== "." &&
    value !== ".."
  )
}

function boundedString(value, max = 256) {
  return typeof value === "string" && value.length > 0 && value.length <= max
}

function validCurrent(value) {
  return (
    value === null ||
    (value &&
      PHASES.has(value.phase) &&
      STEPS.has(value.step) &&
      SOURCES.has(value.source) &&
      (value.role === null || boundedString(value.role)) &&
      boundedString(value.observed_at))
  )
}

function validRole(value) {
  return (
    value &&
    boundedString(value.invocation_id) &&
    Object.hasOwn(ROLES, value.role) &&
    PHASES.has(value.phase) &&
    STEPS.has(value.step) &&
    boundedString(value.started_at)
  )
}

function validTask(value) {
  return value === null || (value && boundedString(value.id) && boundedString(value.status, 64))
}

function validCompletedStep(value) {
  return (
    value &&
    PHASES.has(value.phase) &&
    STEPS.has(value.step) &&
    boundedString(value.completed_at)
  )
}

function validSnapshot(value, contextId) {
  return (
    value &&
    (value.schema_version === 1 || value.schema_version === 2) &&
    value.context_id === contextId &&
    typeof value.activity_active === "boolean" &&
    validTask(value.task) &&
    validCurrent(value.current) &&
    Array.isArray(value.active_roles) &&
    value.active_roles.every(validRole) &&
    (value.schema_version === 1 ||
      (Array.isArray(value.completed_steps) &&
        value.completed_steps.length <= 32 &&
        value.completed_steps.every(validCompletedStep)))
  )
}

function normalizeTask(task) {
  if (!task || !boundedString(task.id) || !boundedString(task.status, 64)) {
    return null
  }
  return { id: task.id, status: task.status }
}

export async function emitStepStart(input, mapping, stateStore, env, client) {
  if (!mapping || !input?.sessionID || !input?.callID ||
      !env.TRELLIS_STEP_START_URL || !env.TRELLIS_STEP_START_TOKEN ||
      !env.TRELLIS_STEP_START_EXECUTION_ID) return
  const [phase, step] = mapping
  const event = {
    type: "workflow_step_started",
    event_id: `${env.TRELLIS_STEP_START_EXECUTION_ID}:${input.sessionID}:${input.callID}:${step}`,
    task_key: stateStore.read()?.task?.id || null,
    phase, step,
    runtime_session_id: input.sessionID,
    tool_call_id: input.callID,
  }
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const response = await fetch(env.TRELLIS_STEP_START_URL, {
        method: "POST",
        headers: { "content-type": "application/json", authorization: `Bearer ${env.TRELLIS_STEP_START_TOKEN}` },
        body: JSON.stringify(event),
        signal: AbortSignal.timeout(1500),
      })
      if (response.ok) return
      debugLog("workflow-state", "Step start rejected:", response.status)
    } catch (error) {
      debugLog("workflow-state", "Step start unavailable:", error instanceof Error ? error.message : String(error))
    }
  }
  try {
    await client?.app?.log?.({ body: { service: "workflow-state", level: "error", message: "WECode step start was not recorded" } })
  } catch {
    debugLog("workflow-state", "Could not report step start failure")
  }
}

export class WorkflowStateStore {
  constructor(directory, options = {}) {
    this.directory = resolve(directory)
    this.enabled = options.enabled === true && safeContextId(options.contextId)
    this.contextId = this.enabled ? options.contextId : null
    this.now = options.now || (() => new Date())
    this.filePath = this.enabled
      ? join(directory, ".trellis", ".runtime", "workflow-state", `${this.contextId}.json`)
      : null
  }

  syncTask(task) {
    if (!this.filePath) return
    const nextTask = normalizeTask(task)
    const state = this.read() || this.emptyState()

    if (!nextTask) {
      if (state.task !== null) {
        if (state.current?.source === "skill" && state.activity_active) {
          this.completeStep(state, state.current.phase, state.current.step)
        }
        state.activity_active = false
        state.active_roles = []
        state.waiting_for = null
      }
      this.write(state)
      return
    }

    if (state.task?.id !== nextTask.id) {
      // Bind the first task without losing the session's preparation activity.
      const next = state.task ? this.emptyState() : state
      next.task = nextTask
      next.waiting_for = null
      next.activity_active = false
      this.write(next)
      return
    }

    const statusChanged = state.task.status !== nextTask.status
    state.task = nextTask
    if (statusChanged && nextTask.status === "completed") {
      if (state.current?.source === "skill" && state.activity_active) {
        this.completeStep(state, state.current.phase, state.current.step)
      }
      state.activity_active = false
      state.active_roles = []
      state.waiting_for = null
      this.write(state)
      return
    }
    if (statusChanged) {
      this.write(state)
    }
  }

  observeSkill(name) {
    const mapping = SKILLS[name]
    const state = this.read() || this.emptyState()
    if (!mapping) { this.resume(state); return }
    this.setCurrent(mapping, "skill", state)
    return name === "trellis-use-case-design" ? undefined : mapping
  }

  startRole(invocationId, role) {
    if (!this.filePath || !boundedString(invocationId) || !Object.hasOwn(ROLES, role)) return
    const state = this.read() || this.emptyState()
    const [phase, step] = ROLES[role]
    if (state.active_roles.some(active => active.invocation_id === invocationId)) return
    this.completePreviousSkill(state, phase, step)
    this.invalidateFollowing(state, phase, step)
    const startedAt = this.timestamp()
    state.active_roles = state.active_roles.filter(active => active.invocation_id !== invocationId)
    state.active_roles.push({
      invocation_id: invocationId,
      round: state.round || 0,
      role,
      phase,
      step,
      started_at: startedAt,
    })
    state.current = {
      phase,
      step,
      source: "subagent",
      role,
      observed_at: startedAt,
      invocation_id: invocationId,
    }
    state.preparation = null
    state.waiting_for = null
    state.activity_active = true
    this.write(state)
    return [phase, step]
  }

  finishRole(invocationId, outcome = "completed") {
    if (!this.filePath || !boundedString(invocationId)) return
    const state = this.read()
    if (!state) return
    const finished = state.active_roles.find(active => active.invocation_id === invocationId)
    const remaining = state.active_roles.filter(active => active.invocation_id !== invocationId)
    if (remaining.length === state.active_roles.length) return
    if (outcome === "completed" && (finished.round || 0) === (state.round || 0)) this.completeStep(state, finished.phase, finished.step)
    state.active_roles = remaining

    // A late terminal event must not overwrite a more recent activity.
    if (state.current?.invocation_id !== invocationId) { this.write(state); return }
    const latest = remaining.at(-1)
    if (latest) {
      state.current = {
        phase: latest.phase,
        step: latest.step,
        source: "subagent",
        role: latest.role,
        observed_at: latest.started_at,
        invocation_id: latest.invocation_id,
      }
      state.activity_active = true
    } else if (state.current) {
      state.current.outcome = outcome
      state.current.role = null
      state.current.observed_at = this.timestamp()
      state.activity_active = false
    }
    this.write(state)
  }

  resume(state = this.read()) {
    if (!state?.current || state.waiting_for || state.current.outcome === "failed" ||
        state.task?.status === "completed") return
    state.activity_active = true
    state.current.observed_at = this.timestamp()
    this.write(state)
  }

  observeTool(name, args = {}) {
    const state = this.read()
    if (!state || state.waiting_for || name === "question") return
    this.resume(state)
    if (!state.current && ["read", "glob", "grep", "list"].includes(name)) {
      this.prepare("project_research", state)
      return
    }
    if (!EDIT_TOOLS.has(name) || state.task?.status !== "in_progress" ||
        state.current?.source !== "skill" || state.current.phase !== "execute" ||
        state.current.step !== "context_loading") return
    const target = [args.filePath, args.file_path, args.path].find(value => typeof value === "string")
    if (target) {
      const path = relative(this.directory, resolve(this.directory, target)).replaceAll("\\", "/")
      if (path === ".." || path.startsWith("../") ||
          path === ".trellis" || path.startsWith(".trellis/") ||
          path === ".opencode" || path.startsWith(".opencode/")) return
    }
    return this.setCurrent(["execute", "implementation"], "tool", state)
  }

  setCurrent(mapping, source, state = this.read()) {
    if (!mapping || !state) return
    const [phase, step] = mapping
    const newlyEntered = state.current?.phase !== phase || state.current?.step !== step
    this.completePreviousSkill(state, phase, step)
    this.invalidateFollowing(state, phase, step)
    state.current = {
      phase,
      step,
      source,
      role: null,
      observed_at: this.timestamp(),
    }
    state.activity_active = true
    state.preparation = null
    state.waiting_for = null
    this.write(state)
    return newlyEntered ? mapping : undefined
  }

  prepare(stage, state = this.read() || this.emptyState()) {
    state.preparation = { stage, observed_at: this.timestamp() }
    state.current = null
    state.waiting_for = null
    state.activity_active = true
    this.write(state)
  }

  waitForQuestion(creation, waitingFor = "input") {
    const state = this.read() || this.emptyState()
    if (creation && !state.task) {
      state.preparation = { stage: "task_creation", observed_at: this.timestamp() }
      state.current = null
    }
    state.waiting_for = creation && !state.task ? "task_confirmation" : waitingFor
    state.activity_active = false
    state.active_roles = []
    this.write(state)
  }

  idle(clearWaiting = false) {
    const state = this.read() || this.emptyState()
    state.activity_active = false
    state.active_roles = []
    if (clearWaiting) state.waiting_for = null
    this.write(state)
  }

  completePreviousSkill(state, phase, step) {
    const current = state.current
    // A real step transition records progression, including after an idle reply.
    // It does not claim that tests passed or that the task is complete.
    if (current?.source === "skill" && current.outcome !== "failed" &&
        (current.phase !== phase || current.step !== step)) {
      this.completeStep(state, current.phase, current.step)
    }
  }

  invalidateFollowing(state, phase, step) {
    const order = ["requirements", "use_case_design", "research", "context_loading", "implementation", "quality_check", "testing", "repair", "spec_update"]
    const index = order.indexOf(step)
    if (index < 0) return
    const previous = state.completed_steps || []
    const invalid = previous.filter(item => order.indexOf(item.step) >= index)
    if (invalid.length) {
      state.previous_round_steps = [...(state.previous_round_steps || []), ...invalid].slice(-32)
      state.completed_steps = previous.filter(item => order.indexOf(item.step) < index)
      state.round = (state.round || 0) + 1
    }
  }

  completeStep(state, phase, step) {
    const completedAt = this.timestamp()
    state.completed_steps = (state.completed_steps || []).filter(
      item => item.phase !== phase || item.step !== step,
    )
    state.completed_steps.push({ phase, step, completed_at: completedAt })
    state.completed_steps = state.completed_steps.slice(-32)
  }

  emptyState() {
    return {
      schema_version: 2,
      context_id: this.contextId,
      activity_active: false,
      task: null,
      current: null,
      active_roles: [],
      completed_steps: [],
      preparation: null,
      waiting_for: null,
    }
  }

  timestamp() {
    return this.now().toISOString()
  }

  read() {
    if (!this.filePath || !existsSync(this.filePath)) return null
    try {
      const value = JSON.parse(readFileSync(this.filePath, "utf-8"))
      if (!validSnapshot(value, this.contextId)) return null
      if (value.schema_version === 1) {
        value.schema_version = 2
        value.completed_steps = []
      }
      if (value.current?.source === "workflow_status") {
        value.current = null
        value.activity_active = false
        value.active_roles = []
      }
      return value
    } catch {
      return null
    }
  }

  write(state) {
    if (!this.filePath) return
    const stateDir = dirname(this.filePath)
    mkdirSync(stateDir, { recursive: true })
    const tempPath = join(stateDir, `.${basename(this.filePath)}.${process.pid}.${randomUUID()}.tmp`)
    try {
      writeFileSync(tempPath, `${JSON.stringify(state, null, 2)}\n`, "utf-8")
      // ponytail: atomic last-writer-wins is enough while one OpenCode process owns a context;
      // add per-context locking only if multiple processes are allowed to share one context ID.
      renameSync(tempPath, this.filePath)
    } finally {
      if (existsSync(tempPath)) unlinkSync(tempPath)
    }
  }
}

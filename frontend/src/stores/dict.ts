import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getDictsApi } from '@/api/system'
import type { Dicts } from '@/types'

export const useDictStore = defineStore('dict', () => {
  const dicts = ref<Dicts | null>(null)
  const loading = ref(false)
  const error = ref('')
  let inflight: Promise<void> | null = null

  const fundTypes = computed(() => dicts.value?.fundTypes ?? [])
  const projectStatuses = computed(() => dicts.value?.projectStatuses ?? [])
  const roles = computed(() => dicts.value?.roles ?? [])
  const techDirections = computed(() => dicts.value?.techDirections ?? [])

  async function load(force = false): Promise<void> {
    if (inflight) {
      return inflight
    }
    if (dicts.value && !force) {
      return
    }
    loading.value = true
    error.value = ''
    inflight = getDictsApi()
      .then((data) => {
        dicts.value = data
      })
      .catch((exception) => {
        error.value = exception instanceof Error ? exception.message : '字典加载失败'
        throw exception
      })
      .finally(() => {
        loading.value = false
        inflight = null
      })
    return inflight
  }

  return {
    dicts,
    loading,
    error,
    fundTypes,
    projectStatuses,
    roles,
    techDirections,
    load
  }
})

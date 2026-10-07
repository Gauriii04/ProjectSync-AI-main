import api from './api.js'

export const getMonitoredProjects = () => api.get('/faculty/projects')

export const getProjectAnalytics = (projectId, sprintId) =>
  api.get(`/faculty/projects/${projectId}/analytics`, { params: sprintId ? { sprintId } : {} })

export const analyzeSprint = (projectId, sprintId) =>
  api.post(`/faculty/projects/${projectId}/sprints/${sprintId}/analyze`)

export const getEvaluationReport = (projectId) =>
  api.get(`/faculty/projects/${projectId}/export-report`, { responseType: 'text' })

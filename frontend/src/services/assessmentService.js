import api from './api.js'

export const getMyAssessment = () => api.get('/students/assessment/me')

export const saveAssessment = (data) => api.post('/students/assessment', data)

export const recommendRoles = (teamId) => api.post(`/teams/${teamId}/recommend-roles`)

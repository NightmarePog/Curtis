export const API_BASE = "/api";

export const DEMO_MODE = process.env.NEXT_PUBLIC_DEMO_MODE === "true";

export const LOGIN_URL = `${API_BASE}/oauth2/authorization/microsoft`;
export const LOGOUT_URL = `${API_BASE}/logout`;

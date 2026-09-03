import { request } from "@/lib/http";

export async function orvalRequest<T>(
  url: string,
  init: RequestInit,
): Promise<T> {
  return request<T>(url, init);
}

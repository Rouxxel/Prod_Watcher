import { QueryClient } from "@tanstack/react-query";
import { createRouter } from "@tanstack/react-router";
import { toastApiError } from "@/lib/api-error";
import { isApiError } from "@/services/api";
import { routeTree } from "./routeTree.gen";

function shouldRetryQuery(failureCount: number, error: unknown): boolean {
  if (isApiError(error)) {
    if (error.status === 0) return failureCount < 2;
    if (error.status === 401 || error.status === 403 || error.status === 429) return false;
  }
  return failureCount < 1;
}

export const getRouter = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: shouldRetryQuery,
        onError: (error) => toastApiError(error, "Could not load data"),
      },
      mutations: {
        onError: (error) => toastApiError(error),
      },
    },
  });

  const router = createRouter({
    routeTree,
    context: { queryClient },
    scrollRestoration: true,
    defaultPreloadStaleTime: 0,
  });

  return router;
};

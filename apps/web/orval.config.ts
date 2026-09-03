import { defineConfig } from "orval";

export default defineConfig({
  curtis: {
    input: {
      target: "../server/build/openapi/openapi.json",
    },
    output: {
      target: "src/generated/api/endpoints.ts",
      schemas: "src/generated/api/models",
      client: "fetch",
      mode: "tags-split",
      clean: true,
      prettier: true,
      override: {
        fetch: {
          includeHttpResponseReturnType: false,
        },
        mutator: {
          path: "src/lib/orval-request.ts",
          name: "orvalRequest",
        },
      },
    },
  },
});

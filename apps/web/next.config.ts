import type { NextConfig } from "next";
import { PHASE_DEVELOPMENT_SERVER } from "next/constants";

const nextConfig = (phase: string): NextConfig => {
  const backendUrl = (
    process.env.CURTIS_BACKEND_URL || "http://localhost:3001"
  ).replace(/\/+$/, "");

  return {
    output: "standalone",
    poweredByHeader: false,
    async rewrites() {
      if (phase !== PHASE_DEVELOPMENT_SERVER) return [];

      return [
        {
          source: "/api/:path*",
          destination: `${backendUrl}/:path*`,
        },
      ];
    },
  };
};

export default nextConfig;

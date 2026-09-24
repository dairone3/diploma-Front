package com.example.dimadyplom2.api

/**
 * Backend connection settings. The default points at a local Cloudflare `wrangler dev`
 * server as seen from the Android emulator (10.0.2.2 == host machine's localhost).
 *
 * Change [baseUrl] to the deployed Worker URL for production.
 */
object BackendConfig {
    /** REST base URL (no trailing slash). Includes the `/api` prefix. */
    const val baseUrl: String = "http://10.0.2.2:8787/api"
}


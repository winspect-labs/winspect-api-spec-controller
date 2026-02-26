# Winspect ApiSpec Controller

Kubernetes operator that watches ApiSpec CRDs and syncs them to the central Winspect platform. When you add or update an ApiSpec in your cluster, the controller automatically creates or updates the API and spec record in Winspect.

## Features

- **Watch ApiSpec CRDs** — Monitors ApiSpec resources across namespaces
- **Sync to central API** — Creates or updates API + SpecRecord via `POST /api-management/discovery/spec-records/sync`
- **Inline and ConfigMapRef** — Supports both `definition.inline` and `definition.configMapRef`
- **Delete behavior** — When ApiSpec is deleted, the API and spec record remain unchanged (per design)

## Configuration

Configure via environment variables or `application.yml`:

| Variable | Description |
|----------|-------------|
| `APISPEC_ORG_ID` | Organization ID (required) |
| `APISPEC_API_KEY` | API key for central platform (required) |
| `APISPEC_CENTRAL_API_URL` | Central API base URL (required) |
| `APISPEC_CLUSTER_ID` | Cluster identifier (optional) |
| `APISPEC_SYNC_INTERVAL_MS` | Sync interval in ms (default: 60000) |

## Installation

1. Install the ApiSpec CRD:
   ```bash
   kubectl apply -f crds/apispec-crd.yaml
   ```

2. Create namespace and RBAC:
   ```bash
   kubectl create namespace winspect-system
   kubectl apply -f manifests/rbac.yaml
   ```

3. Deploy the controller (e.g. via Helm or raw manifests) with the required env vars.

## Usage with winspect-cli

In your CI/CD pipeline, use `winspect resolve` to resolve tokens and embed spec content before applying:

```bash
winspect resolve --spec api-spec.yaml | kubectl apply -f -
```

---

See the [ApiSpec CRD and Winspect Operator Plan](.cursor/plans/apispec_crd_and_winspect_operator_4d78f31b.plan.md) for full architecture.

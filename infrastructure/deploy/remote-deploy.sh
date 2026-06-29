#!/usr/bin/env bash
# Remote deploy script — run on the server via SSH from GitHub Actions.
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/sauda}"
IMAGE_DIR="${IMAGE_DIR:-/tmp/sauda-deploy}"
SAUDA_DEPLOY_MODE="${SAUDA_DEPLOY_MODE:-transfer}"

compose_cmd() {
    if docker compose version >/dev/null 2>&1; then
        echo "docker compose"
    elif command -v docker-compose >/dev/null 2>&1; then
        echo "docker-compose"
    else
        echo "Docker Compose not found. Install: apt install -y docker-compose-plugin" >&2
        exit 1
    fi
}

login_ghcr() {
    if [[ -n "${GHCR_USERNAME:-}" && -n "${GHCR_TOKEN:-}" ]]; then
        echo "Logging in to ghcr.io..."
        echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
    else
        echo "GHCR_USERNAME/GHCR_TOKEN not set — pull may fail for private images." >&2
    fi
}

pull_with_retry() {
    local compose="$1"
    local attempt
    for attempt in 1 2 3; do
        echo "docker compose pull (attempt ${attempt}/3)..."
        if $compose pull; then
            return 0
        fi
        echo "Pull failed, retrying in 15s..." >&2
        sleep 15
    done
    return 1
}

sync_repo_config() {
    if [[ ! -d "$DEPLOY_DIR/.git" ]]; then
        echo "Deploy dir is not a git repo — skipping config sync (nginx/compose stay as on disk)." >&2
        return 0
    fi

    local ref="${DEPLOY_GIT_REF:-}"
    echo "Syncing repo config (nginx, compose) from origin${ref:+ ref=$ref}..."
    git -C "$DEPLOY_DIR" fetch --prune origin

    if [[ -n "$ref" ]]; then
        git -C "$DEPLOY_DIR" checkout "$ref"
        git -C "$DEPLOY_DIR" pull --ff-only origin "$ref"
    else
        git -C "$DEPLOY_DIR" pull --ff-only
    fi
}

load_transferred_images() {
    if [[ ! -d "$IMAGE_DIR" ]]; then
        echo "Image directory not found: $IMAGE_DIR" >&2
        exit 1
    fi
    shopt -s nullglob
    local archives=("$IMAGE_DIR"/*.tar.gz)
    if [[ ${#archives[@]} -eq 0 ]]; then
        echo "No image archives in $IMAGE_DIR" >&2
        exit 1
    fi
    for archive in "${archives[@]}"; do
        echo "Loading $(basename "$archive")..."
        gunzip -c "$archive" | docker load
    done
    rm -f "${archives[@]}"
}

main() {
    local compose
    compose="$(compose_cmd)"

    if [[ ! -d "$DEPLOY_DIR" ]]; then
        echo "Deploy directory not found: $DEPLOY_DIR" >&2
        exit 1
    fi

    cd "$DEPLOY_DIR"

    sync_repo_config

    case "$SAUDA_DEPLOY_MODE" in
        transfer)
            echo "Deploy mode: transfer (images loaded from $IMAGE_DIR)"
            load_transferred_images
            ;;
        pull)
            echo "Deploy mode: pull from GHCR"
            login_ghcr
            if ! pull_with_retry "$compose"; then
                echo "::error::GHCR pull failed (timeout/network). Use DEPLOY_IMAGE_TRANSFER=true in GitHub Variables." >&2
                exit 1
            fi
            ;;
        *)
            echo "Unknown SAUDA_DEPLOY_MODE: $SAUDA_DEPLOY_MODE" >&2
            exit 1
            ;;
    esac

    echo "Restarting stack..."
    $compose down --remove-orphans
    $compose up -d --no-build

    # Bind-mounted nginx.conf is read only at container start — recreate edge nginx
    # so /swagger-ui.html and /v3/api-docs routes apply after git pull.
    echo "Recreating edge nginx..."
    $compose up -d --no-deps --force-recreate nginx
    $compose exec -T nginx nginx -t

    if $compose exec -T nginx wget -qO- http://127.0.0.1/swagger-ui.html 2>/dev/null | grep -q 'vite.svg'; then
        echo "::warning::/swagger-ui.html is still routed to the frontend SPA." >&2
        echo "Check: docker compose ps, NGINX_HTTP_PORT in .env, host nginx on :80." >&2
    else
        echo "Swagger UI routing: OK"
    fi

    echo "Deploy complete."
    $compose ps
}

main "$@"

import { useAuth } from "../../../auth/AuthProvider";

export function useDistributorPermissions() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const isManager = roles.includes("distributor_manager");
  const isViewer = roles.includes("distributor_viewer");
  const canManageMatches = isManager;
  const canReadNotifications = isManager;

  return { isManager, isViewer, canManageMatches, canReadNotifications };
}

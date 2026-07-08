import { useAuth } from "../../../auth/AuthProvider";

export function useAdminPermissions() {
  const { user } = useAuth();
  const isPlatformAdmin = user?.organizationType === "platform";
  const canReadImports = isPlatformAdmin;

  return { isPlatformAdmin, canReadImports };
}

/** Extract Supabase signup confirmation token from redirect query or hash. */
export function extractSignupConfirmationToken(search: string, hash: string): string | null {
  const params = new URLSearchParams(search);
  const fromQuery = params.get("token_hash") ?? params.get("token");
  if (fromQuery) return fromQuery;

  if (!hash) return null;
  const hashParams = new URLSearchParams(hash.startsWith("#") ? hash.slice(1) : hash);
  return hashParams.get("token_hash") ?? hashParams.get("token");
}

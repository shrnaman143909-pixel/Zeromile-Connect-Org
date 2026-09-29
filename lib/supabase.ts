// Zeromile Connect - Supabase Client Configuration
// Safe browser client using publishable/anon key only.
// Service role key is NEVER exposed to the client.

const SUPABASE_URL = 
  (typeof process !== 'undefined' && process.env?.SUPABASE_URL) ||
  (typeof import.meta !== 'undefined' && (import.meta as any).env?.VITE_SUPABASE_URL) ||
  '';

const SUPABASE_PUBLISHABLE_KEY = 
  (typeof process !== 'undefined' && process.env?.SUPABASE_PUBLISHABLE_KEY) ||
  (typeof import.meta !== 'undefined' && (import.meta as any).env?.VITE_SUPABASE_PUBLISHABLE_KEY) ||
  '';

export const isSupabaseConfigured = Boolean(
  SUPABASE_URL && 
  SUPABASE_PUBLISHABLE_KEY && 
  !SUPABASE_URL.includes('placeholder-project')
);

export const supabaseConfig = {
  url: SUPABASE_URL,
  key: SUPABASE_PUBLISHABLE_KEY,
};

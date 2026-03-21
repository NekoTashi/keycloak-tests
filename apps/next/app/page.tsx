"use client";

import { useSession, signInWithKeycloak, signOut } from "@/lib/auth-client";

export default function Home() {
  const { data: session, isPending } = useSession();

  if (isPending) return <p>Loading&hellip;</p>;

  if (!session) {
    return (
      <div>
        <h1>Keycloak + Better Auth (Stateless)</h1>
        <p>No database &mdash; sessions stored in encrypted cookies.</p>
        <button onClick={() => signInWithKeycloak()}>
          Login with Keycloak
        </button>
      </div>
    );
  }

  return (
    <div>
      <h1>Authenticated</h1>
      <p>
        Welcome, <strong>{session.user.name || session.user.email}</strong>
      </p>
      <pre>{JSON.stringify(session.user, null, 2)}</pre>
      <nav style={{ display: "flex", gap: "1rem", marginTop: "1rem" }}>
        <a href="/protected">Protected Page (SSR)</a>
        <button onClick={() => signOut()}>Logout</button>
      </nav>
    </div>
  );
}

import { auth } from "@/lib/auth";
import { headers } from "next/headers";
import { redirect } from "next/navigation";

export default async function ProtectedPage() {
  const session = await auth.api.getSession({
    headers: await headers(),
  });

  if (!session) redirect("/");

  return (
    <div>
      <h1>Protected Page (Server-Side)</h1>
      <p>This page checked your session on the server before rendering.</p>
      <dl>
        <dt>Name</dt>
        <dd>{session.user.name}</dd>
        <dt>Email</dt>
        <dd>{session.user.email}</dd>
      </dl>
      <pre>{JSON.stringify(session, null, 2)}</pre>
      <a href="/">&larr; Home</a>
    </div>
  );
}

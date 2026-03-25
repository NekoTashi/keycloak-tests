import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { AuthProvider, useAuth } from "react-oidc-context";

const oidcConfig = {
  authority: "http://localhost:8080/realms/ripley",
  client_id: "react-document-number-app",
  redirect_uri: "http://localhost:5173",
  response_type: "code",
  scope: "openid profile email",
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};

function App() {
  const auth = useAuth();

  if (auth.isLoading) return <p>Loading…</p>;
  if (auth.error) return <p>Error: {auth.error.message}</p>;

  if (!auth.isAuthenticated) {
    return (
      <div>
        <h1>Keycloak PKCE Demo</h1>
        <button onClick={() => auth.signinRedirect()}>
          Login with Keycloak
        </button>
      </div>
    );
  }

  return (
    <div>
      <h1>Authenticated</h1>
      <button
        onClick={() =>
          auth.signoutRedirect({
            post_logout_redirect_uri: "http://localhost:5173",
          })
        }
      >
        Logout
      </button>
      {auth.user && <pre>{JSON.stringify(auth.user.profile, null, 2)}</pre>}
    </div>
  );
}

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AuthProvider {...oidcConfig}>
      <App />
    </AuthProvider>
  </StrictMode>,
);

package com.ripley.keycloak;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;

// Authenticates users by their "document_number" attribute instead of username/email.
public class DocumentNumberAuthenticator extends UsernamePasswordForm {

    private static final String DOCUMENT_NUMBER_ATTRIBUTE = "document_number";

    @Override
    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        // Read the document number from the login form's username field
        String documentNumber = formData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (documentNumber == null || documentNumber.isBlank()) {
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = challenge(context, Messages.INVALID_USER);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return false;
        }

        // Record the attempted document number for event logging and session tracking
        documentNumber = documentNumber.trim();
        context.getEvent().detail(Details.USERNAME, documentNumber);
        context.getAuthenticationSession().setAuthNote(
                AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, documentNumber);

        // Look up the user by the document_number custom attribute
        UserModel user = context.getSession().users()
                .searchForUserByUserAttributeStream(context.getRealm(), DOCUMENT_NUMBER_ATTRIBUTE, documentNumber)
                .findFirst()
                .orElse(null);

        if (user == null) {
            testInvalidUser(context, user);
            return false;
        }

        // Reject disabled or brute-force-locked accounts
        if (!enabledUser(context, user)) {
            return false;
        }

        // Validate the submitted password against the matched user's credentials
        String password = formData.getFirst(CredentialRepresentation.PASSWORD);
        if (password == null || password.isEmpty()
                || !user.credentialManager().isValid(UserCredentialModel.password(password))) {
            context.getEvent().user(user);
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = challenge(context, Messages.INVALID_USER);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            return false;
        }

        // Bind the authenticated user to the flow context
        context.setUser(user);
        return true;
    }
}

package org.vaadin.addon.oauthpopup;

import java.io.IOException;

import org.scribe.model.Token;
import org.scribe.model.Verifier;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

/**
 * Handles the callback from the OAuth authorization url.
 * <p>
 * When done, closes the window and removes this handler.
 *
 */
@SuppressWarnings("serial")
public class OAuthCallbackRequestHandler implements RequestHandler {

	private final Token requestToken;
	private final OAuthData data;

	private static final String CLOSE_WINDOW_HTML =
			"<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"/>" +
			"<script>window.close();</script>" +
			"</head><body>" +
			"</body></html>";

	/**
	 * Only handles request that match the data id.
	 * 
	 * @param requestToken may be null (in case of OAuth2)
	 * @param data
	 */
	public OAuthCallbackRequestHandler(Token requestToken, OAuthData data) {
		this.requestToken = requestToken;
		this.data = data;
	}

	@Override
	public boolean handleRequest(VaadinSession session,
			VaadinRequest request, VaadinResponse response) throws IOException {
		
		if (!data.isCallbackForMe(request)) {
			return false;
		}

		String verifier = request.getParameter(data.getVerifierParameterName());
		if (verifier != null) {
			// Got verifier!
			data.setVerifier(requestToken, new Verifier(verifier));
			finish(session, response);
			return true;
		}
		
		// No verifier in the parameters. That's most likely because the user
		// denied the OAuth.
		
		// TODO: current error message reporting (below) is not very useful
		
		String error = null;
		for (String errorName : data.getErrorParameterNames()) {
			error = request.getParameter(errorName);
			if (error != null) {
				break;
			}
		}
		
		String errorMessage;
		if (error==null) {
			errorMessage = "OAuth failed.";
		}
		else {
			errorMessage = "OAuth denied: " + error;
		}
		
		data.setDenied(errorMessage);
		finish(session, response);
		return true;
	}

	private void finish(VaadinSession session, VaadinResponse response) throws IOException {
		response.setContentType("text/html");
		response.getWriter().append(CLOSE_WINDOW_HTML);
		cleanUpSession(session);
	}
	
	// NOTE: the finish() method above is not called if the user
	// doesn't browse back from the OAuth authentication page.
	// That's why we make cleanUpSession public so that others can clean up also.
	public void cleanUpSession(final VaadinSession session) {
		session.access(new Runnable() {
			@Override
			public void run() {
				session.removeRequestHandler(OAuthCallbackRequestHandler.this);
			}
		});
	}

}

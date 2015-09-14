package de.marcweinberger.domain.service;

import de.marcweinberger.data.repository.UserRepository;
import de.marcweinberger.domain.gateway.GithubAuthGateway;
import de.marcweinberger.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2ClientProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Business service for authentication with 3rd party auth providers (like Github).
 *
 * @author Marc Weinberger, marc.weinberger@me.com
 * @since 14.09.15
 */
@Service
public class AuthService {
  private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

  @Autowired
  private GithubAuthGateway githubAuthGateway;
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AuthorizationServerTokenServices authorizationServerTokenServices;
  @Autowired
  private OAuth2ClientContext clientContext;
  @Autowired
  private OAuth2ClientProperties clientProperties;

  public OAuth2AccessToken authorizeGithubUser(String authorizationCode, String redirectURI) {
    logger.info("starting github authorization with authCode: {}, redirectUri: {}", authorizationCode, redirectURI);
    enhanceOAuth2ClientContext(authorizationCode, redirectURI);

    final String githubUserEmail = getGithubUserEmail();
    final User user = getUser(githubUserEmail);

    return createAccessToken(githubUserEmail, user);
  }

  private void enhanceOAuth2ClientContext(String authorizationCode, String redirectURI) {
    if (StringUtils.hasLength(authorizationCode)) {
      final AccessTokenRequest accessTokenRequest = clientContext.getAccessTokenRequest();
      accessTokenRequest.setAuthorizationCode(authorizationCode);
      accessTokenRequest.setCurrentUri(redirectURI);
    }
  }

  private String getGithubUserEmail() {
    return githubAuthGateway.getGithubEmail();
  }

  @Transactional
  private User getUser(String githubUserEmail) {
    // lookup user or create user
    final Optional<User> optionalUser = userRepository.findByEmail(githubUserEmail);
    return optionalUser.orElseGet(() -> {
      logger.info("creating user for email: {}", githubUserEmail);
      return userRepository.save(new User(githubUserEmail));
    });
  }

  private OAuth2AccessToken createAccessToken(String githubUserEmail, User user) {
    logger.info("creating token for user: {}", user);
    final List<GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER");
    final OAuth2Request request = new OAuth2Request(null, clientProperties.getClientId(), null, true, null, null, null, null, null);
    final OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(request, new UsernamePasswordAuthenticationToken(githubUserEmail, "N/A", authorities));

    return authorizationServerTokenServices.createAccessToken(oAuth2Authentication);
  }
}

package tn.sia.b2b.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tn.sia.b2b.identity.service.SessionService;

import java.io.IOException;
import java.util.List;

public class SessionAuthFilter extends OncePerRequestFilter {

    private static final String SESSION_COOKIE = "sia_session";

    private final SessionService sessionService;

    public SessionAuthFilter(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            SessionService.SessionData session = sessionService.getSession(token);
            if (session != null) {
                var auth = new UsernamePasswordAuthenticationToken(
                        session.userId().toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + session.role()))
                );
                auth.setDetails(session);
                SecurityContextHolder.getContext().setAuthentication(auth);
                request.setAttribute("sessionData", session);
            }
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (SESSION_COOKIE.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}

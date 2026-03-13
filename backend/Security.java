package com.smartops;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.*;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.*;
import java.io.IOException;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

// ─── UserDetailsImpl ──────────────────────────────────────────────────────────

@AllArgsConstructor @Getter
class UserDetailsImpl implements UserDetails {
    private Long id;
    private String employeeId;
    private String email;
    private String firstName;
    private String lastName;
    @JsonIgnore private String password;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> auths = user.getRoles().stream()
            .map(r -> new SimpleGrantedAuthority(r.getName().name()))
            .collect(Collectors.toList());
        return new UserDetailsImpl(user.getId(), user.getEmployeeId(), user.getEmail(),
            user.getFirstName(), user.getLastName(), user.getPassword(), auths);
    }

    @Override public String getUsername()              { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}

// ─── UserDetailsServiceImpl ───────────────────────────────────────────────────

@Service @RequiredArgsConstructor
class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return UserDetailsImpl.build(user);
    }
}

// ─── JwtUtils ─────────────────────────────────────────────────────────────────

@Component
class JwtUtils {
    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret}")   private String jwtSecret;
    @Value("${app.jwt.expiration-ms}") private int jwtExpirationMs;

    private Key key() {
        return Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(Base64.getEncoder().encodeToString(jwtSecret.getBytes())));
    }

    public String generateJwtToken(Authentication auth) {
        UserDetailsImpl u = (UserDetailsImpl) auth.getPrincipal();
        return Jwts.builder().setSubject(u.getEmail()).setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(key(), SignatureAlgorithm.HS256).compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
            .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try { Jwts.parserBuilder().setSigningKey(key()).build().parse(token); return true; }
        catch (Exception e) { log.error("JWT error: {}", e.getMessage()); return false; }
    }
}

// ─── AuthTokenFilter ──────────────────────────────────────────────────────────

@Component @RequiredArgsConstructor
class AuthTokenFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = req.getHeader("Authorization");
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                String jwt = header.substring(7);
                if (jwtUtils.validateToken(jwt)) {
                    UserDetails ud = userDetailsService.loadUserByUsername(jwtUtils.getEmailFromToken(jwt));
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) { logger.error("Auth error: " + e.getMessage()); }
        chain.doFilter(req, res);
    }
}

// ─── SecurityConfig ───────────────────────────────────────────────────────────

@Configuration @EnableWebSecurity @EnableMethodSecurity @RequiredArgsConstructor
class SecurityConfig {
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthTokenFilter authTokenFilter;

    @Value("${app.cors.allowed-origins}") private String allowedOrigins;

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .cors(c -> c.configurationSource(corsSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/auth/login").permitAll().anyRequest().authenticated());
        http.authenticationProvider(authProvider());
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}

// ─── DataInitializer ──────────────────────────────────────────────────────────

@Component @RequiredArgsConstructor
class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        for (Role.ERole r : Role.ERole.values())
            if (roleRepo.findByName(r).isEmpty())
                roleRepo.save(Role.builder().name(r).build());

        if (!userRepo.existsByEmail("admin@smartops.com")) {
            Role adminRole = roleRepo.findByName(Role.ERole.ROLE_ADMIN).orElseThrow();
            userRepo.save(User.builder()
                .employeeId("EMP001").firstName("System").lastName("Admin")
                .email("admin@smartops.com").password(encoder.encode("Admin@123"))
                .department("IT").designation("Administrator").status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole))).build());
            System.out.println("✅ Admin created → admin@smartops.com / Admin@123");
        }
    }
}

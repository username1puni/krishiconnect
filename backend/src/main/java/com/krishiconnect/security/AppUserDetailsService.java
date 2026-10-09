package com.krishiconnect.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.krishiconnect.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /** The token subject is the user id. */
    @Override
    public UserDetails loadUserByUsername(String id) {
        try {
            return userRepository.findById(Long.valueOf(id))
                    .map(AuthUser::new)
                    .orElseThrow(() -> new UsernameNotFoundException("No user " + id));
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Bad user id " + id);
        }
    }
}

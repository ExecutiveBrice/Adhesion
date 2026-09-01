package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.repository.RoleRepository;
import com.wild.corp.adhesion.repository.SeanceRepository;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.models.resources.SeanceDuJourResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class UserServices {
    @Autowired
    UserRepository userRepository;
    @Autowired
    SeanceRepository seanceRepository;
    @Autowired
    ConfirmationTokenService confirmationTokenService;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    EmailService emailService;
    @Autowired
    AdherentServices adherentServices;
    @Autowired
    PasswordEncoder encoder;
    @Value("${server.name:localhost:8002}")
    private String serverName;


    public List<SeanceDuJourResponse> getSeancesDuJourForUser(String username) {
        LocalDate today = LocalDate.now();
        return seanceRepository.findTodayByProfessorUsername(
                        username, today.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                .map(SeanceDuJourResponse::from)
                .toList();
    }

    /** Returns every session planned on a given day for the secretariat view. */
    public List<SeanceDuJourResponse> getSeancesDuJourForSecretary(LocalDate date) {
        return seanceRepository.findAllByDebutGreaterThanEqualAndDebutLessThanOrderByDebut(
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay()).stream()
                .map(SeanceDuJourResponse::from)
                .toList();
    }



    public boolean existsByEmail(String email) {
        return userRepository.existsByUsername(email);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findByEmail(String email) {
        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));
        return user;
    }

    public Adherent createUserAnonymous(String email) {
        Random random = new Random();
        String password = random.toString();
        User user = addNewUser(email.toLowerCase(), password);
        Adherent adherent = adherentServices.newAdherent(null, true);
        adherent.setUser(user);
        adherentServices.save(adherent);

        EmailContent mess = new EmailContent();
        mess.getDestinataires().add(user.getUsername());
        mess.setSubject("Inscription ALOD");
        mess.setText("Bonjour,<br>" +
                "Notre secrétariat vous à ajouté manuellement dans notre outil de suivi des adhésions,<br>" +
                "vous pouvez dors et déjà vous inscrire aux activités de votre choix<br><br>" +
                "Cordialement,<br>" +
                "l'équipe de l'ALOD");
        emailService.sendMessage(mess);
        return adherent;
    }

    public User createNewUser(String email, String cryptedPassword) {
        User user = addNewUser(email.toLowerCase(), cryptedPassword);

        Adherent adherent = adherentServices.newAdherent(null, true);
        adherent.setUser(user);
        adherentServices.save(adherent);

        confirmEmailAsking(user);

        return user;
    }

    public User addNewUser(String email, String password) {
        // Create new user's account
        User user = new User(email.toLowerCase(), encoder.encode(password));
        // Create new user's account
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.getRoles().add(userRole);
        user = userRepository.save(user);

        return user;
    }

    public User grantUser(ERole role, User user) {
        Role userRole = roleRepository.findByName(role)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        user.getRoles().add(userRole);
        return userRepository.save(user);
    }

    public User unGrantUser(ERole role, User user) {
        Role userRole = roleRepository.findByName(role)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        user.getRoles().remove(userRole);
        return userRepository.save(user);
    }

    public void confirmEmailAnswer(String token) throws Exception {
        Instant now = Instant.now();
        ConfirmationToken confirmationToken = confirmationTokenService.consume(
                token, ConfirmationTokenType.EMAIL_CONFIRMATION, now);
        final User user = confirmationToken.getUser();
        user.setEmailValid(true);
        userRepository.saveAndFlush(user);
        confirmationTokenService.invalidateAll(user, ConfirmationTokenType.EMAIL_CONFIRMATION, now);
    }

    public void confirmEmailAsking(User user) {
        Instant now = Instant.now();
        confirmationTokenService.invalidateAll(user, ConfirmationTokenType.EMAIL_CONFIRMATION, now);
        String rawToken = confirmationTokenService.create(
                user, ConfirmationTokenType.EMAIL_CONFIRMATION, Duration.ofHours(24), now);
        EmailContent mess = new EmailContent();
        mess.getDestinataires().add(user.getUsername());
        mess.setSubject("Confirmation Email");
        mess.setText("Bonjour,<br>" +
                "Ceci est le <a href=https://" + serverName + "/api_adhesion/auth/confirmEmail/" + rawToken + ">lien de confirmation de votre adresse mail</a><br><br>" +
                "Vous pouvez dors et déjà vous inscrire aux activités de votre choix<br><br>" +
                "Cordialement,<br>" +
                "l'équipe de l'ALOD");
        emailService.sendMessage(mess);
    }


    public void isUserExist(String email) {
        findByEmail(email);
    }


//pour les tests en local
    public void changeTestPassword() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> user.setPassword(encoder.encode("testPass")));
        userRepository.saveAll(users);
    }

    public void deleteuser(User user) {

        user.getTokens().clear();
        user.getNotifs().clear();
        user.getRoles().clear();

        userRepository.delete(user);
    }



    public List<UserLite> getAllLite() {
        return userRepository.findAll().stream().map(this::reduceUser).collect(Collectors.toList());
    }
    public void getAllAlone() {
        List<User> users = userRepository.findAll();


        userRepository.deleteAll(users.stream().filter(user -> user.getAdherent() == null).toList());

    }


    private UserLite reduceUser(User user){
        log.debug(user.getId().toString());
        UserLite userLite = new UserLite();
        userLite.setId(user.getId());
        if(user.getAdherent() == null){
            log.error("pas d'adhérent pour ce user :"+user.getId());
        }else{
            userLite.setAdherent(user.getAdherent().getPrenom()+" "+user.getAdherent().getNom());
        }

        userLite.setRoles(user.getRoles());
        userLite.setUsername(user.getUsername());
        return userLite;
    }


}

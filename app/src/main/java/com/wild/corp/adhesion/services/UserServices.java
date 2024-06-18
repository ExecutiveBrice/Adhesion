package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.repository.RoleRepository;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServices {
    @Autowired
    UserRepository userRepository;
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
        mess.setDiffusion(user.getUsername());
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
        ConfirmationToken confirmationToken = confirmationTokenService.findByToken(token);
        final User user = confirmationToken.getUser();
        user.setEmailValid(true);
        userRepository.save(user);
        confirmationTokenService.deleteConfirmationToken(confirmationToken.getId());
    }

    public void confirmEmailAsking(User user) {
        ConfirmationToken cft = new ConfirmationToken();
        cft.setUser(user);
        cft.setCreatedDate(LocalDate.now());
        UUID uuid = UUID.randomUUID();
        cft.setConfirmationToken(uuid);
        cft.setType("ConfirmationEmail");
        EmailContent mess = new EmailContent();
        mess.setDiffusion(user.getUsername());
        mess.setSubject("Confirmation Email");
        mess.setText("Bonjour,<br>" +
                "Ceci est le <a href=https://" + serverName + "/api_adhesion/auth/confirmEmail/" + uuid + ">lien de confirmation de votre adresse mail</a><br><br>" +
                "Vous pouvez dors et déjà vous inscrire aux activités de votre choix<br><br>" +
                "Cordialement,<br>" +
                "l'équipe de l'ALOD");
        emailService.sendMessage(mess);

        confirmationTokenService.saveConfirmationToken(cft);
    }


    public void isUserExist(String email) {
        findByEmail(email);
    }


    public ConfirmationToken reinitPassword(String email) {
        User user = findByEmail(email);

        ConfirmationToken cft = new ConfirmationToken();
        cft.setUser(user);
        cft.setCreatedDate(LocalDate.now());
        UUID uuid = UUID.randomUUID();
        cft.setConfirmationToken(uuid);
        cft.setType("ReinitPassword");

        confirmationTokenService.saveConfirmationToken(cft);

        EmailContent mess = new EmailContent();
        mess.setDiffusion(user.getUsername());
        mess.setSubject("Réinitialisation du mot de passe");
        mess.setText("Bonjour,<br>" +
                "Ceci est le <a href=https://" + serverName + "/adhesion/#/resetPassword/" + uuid + ">lien de renouvellement de votre mot de passe</a><br>" +
                "Cordialement,<br>" +
                "l'équipe de l'ALOD");

        log.info(mess.getDiffusion());
        emailService.sendMessage(mess);
        return cft;
    }

//pour les tests en local
    public void changeTestPassword() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> user.setPassword(encoder.encode("testPass")));
        userRepository.saveAll(users);
    }

    public void changePassword(String token, String password) {
        ConfirmationToken confirmationToken = confirmationTokenService.findByToken(token);
        final User user = confirmationToken.getUser();
        user.setPassword(encoder.encode(password));
        userRepository.save(user);
        confirmationTokenService.deleteConfirmationToken(confirmationToken.getId());
    }

    public List<UserLite> getAllLite() {
        return userRepository.findAll().stream().map(this::reduceUser).collect(Collectors.toList());
    }

    private UserLite reduceUser(User user){
        log.info(user.getId().toString());
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

package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.repository.RoleRepository;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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
    AccordServices accordServices;
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

    public void addUserForAll() {
        List<Adherent> adherents = adherentServices.getAll();

        adherents.forEach(adherent -> {
            if (adherent.getUser() == null) {
                addUserToAdherent(adherent);
            }
        });
    }

    public User addUserToAdherent(Adherent adherent) {
        Random random = new Random();
        String password = random.toString();
        User user;
        String username = adherent.getPrenom() + adherent.getNom();
        int compteur = 1;
        while (userRepository.findByUsername(username).isPresent()) {
            username = adherent.getPrenom() + adherent.getNom() + "_" + compteur;
            compteur++;
        }

        user = new User(username, encoder.encode(password));
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.getRoles().add(userRole);
        user = userRepository.save(user);
        adherent.setUser(user);
        adherentServices.save(adherent);
        return user;
    }

    public User findById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with id: " + userId));
        return user;
    }

    public User updateUsername(String newEmail, User user) {
        user.setUsername(newEmail);
        userRepository.save(user);
        return user;
    }

    public Adherent createUserAnonymous(String email) {
        Random random = new Random();
        //String password = random.toString();
        String password = "testPass";
        User user = addNewUser(email.toLowerCase(), encoder.encode(password));
        Adherent adherent = adherentServices.newAdherent(null, true);
        adherent.setUser(user);
        adherentServices.save(adherent);

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

    public User addNewUser(String email, String cryptedPassword) {
        // Create new user's account
        User user = new User(email.toLowerCase(), cryptedPassword);
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
        Email mess = new Email();
        mess.setDiffusion(user.getUsername());
        mess.setSubject("Confirmation Email");
        mess.setText("Bonjour,<br>" +
                "Ceci est le <a href=https://" + serverName + "/api_adhesion/auth/confirmEmail/" + uuid + ">lien de confirmation de votre adresse mail</a><br>" +
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

        Email mess = new Email();
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

    public void deleteById(Long userId) {
        userRepository.deleteById(userId);
    }

    public List<UserLite> getAllLite() {
        return userRepository.findAll().stream().map(this::reduceUser).collect(Collectors.toList());
    }

    private UserLite reduceUser(User user){
        UserLite userLite = new UserLite();
        userLite.setId(user.getId());
        userLite.setAdherent(user.getAdherent().getPrenom()+" "+user.getAdherent().getNom());
        userLite.setRoles(user.getRoles());
        userLite.setUsername(user.getUsername());
        return userLite;
    }

    public void initAdmin() {
        User user = createNewUser("admin", encoder.encode("adminPass"));
        grantUser(ERole.ROLE_ADMIN, user);

    }
}

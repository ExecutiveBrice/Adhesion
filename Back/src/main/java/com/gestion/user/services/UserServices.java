package com.gestion.user.services;

import com.gestion.auth.services.ConfirmationTokenService;
import com.gestion.emails.services.EmailService;
import com.gestion.user.models.*;
import com.gestion.user.repository.RoleRepository;
import com.gestion.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
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
    TribuServices tribuServices;
    @Autowired
    PasswordEncoder encoder;
    @Value("${server.name:localhost:8002}")
    private String serverName;

    public boolean existsByEmail(String email){
        return userRepository.existsByUsername(email);
    }

    public List<User> findAll(){
        return userRepository.findAll();
    }
    public User findByEmail(String email){
        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));
        return user;
    }

    public User updateUsername(String newEmail, String oldEmail){
        User user = userRepository.findByUsername(oldEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + oldEmail));
        user.setUsername(newEmail);
        userRepository.save(user);
        return user;
    }

    public User createNewUser(String email, String cryptedPassword){
        // Create new user's account
        User user = new User(email.toLowerCase(),cryptedPassword);
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.getRoles().add(userRole);
        user = userRepository.save(user);

        Set<Adherent> adherents = new HashSet<>();
        Adherent adherent = new Adherent();
        adherent.setEmail(user.getUsername().toLowerCase());
        adherent.setReferent(true);
        adherent.setAdresseReferent(false);
        adherent.setEmailReferent(false);
        adherent.setTelephoneReferent(false);

        Accord accordRgpd = new Accord("RGPD");
        accordRgpd.setEtat(true);
        accordRgpd.setDatePassage(LocalDate.now());
        adherent.getAccords().add(accordRgpd);

        adherent.getAccords().add(new Accord("Droit Image"));
        adherent.setUser(user);
        adherentServices.save(adherent, null);
        confirmEmailAsking(user);

        return user;
    }

    public User grantUser(ERole role, User user){
        Role userRole = roleRepository.findByName(role)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        user.getRoles().add(userRole);
        return userRepository.save(user);
    }

    public User unGrantUser(ERole role, User user){
        Role userRole = roleRepository.findByName(role)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        user.getRoles().remove(userRole);
        return userRepository.save(user);
    }

    public List<UserLite> getUserByRole(ERole role){
        List<User> users = userRepository.findAll();
        Role userRole = roleRepository.findByName(role)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        List<UserLite> userLites = users.stream().filter(user -> user.getRoles().contains(userRole))
                .map(user -> new UserLite(user)).collect(Collectors.toList());
        return userLites;
    }



    public void confirmEmailAnswer(String token) throws Exception {
        ConfirmationToken confirmationToken= confirmationTokenService.findByToken(token);
            final User user = confirmationToken.getUser();
            user.setEmailValid(true);
            userRepository.save(user);
            confirmationTokenService.deleteConfirmationToken(confirmationToken.getId());
    }

    public void confirmEmailAsking(User user){
        ConfirmationToken cft = new ConfirmationToken();
        cft.setUser(user);
        cft.setCreatedDate(LocalDate.now());
        UUID uuid = UUID.randomUUID();
        cft.setConfirmationToken(uuid);
        cft.setType("ConfirmationEmail");
        SimpleMailMessage mess = new SimpleMailMessage();
        mess.setTo(user.getUsername());
        mess.setSubject("Confirmation Email");
        mess.setText("Bonjour,<br>" +
                "Ceci est le <a href=https://"+serverName+"/api/auth/confirmEmail/"+uuid+">lien de confirmation de votre adresse mail</a><br>" +
                "Cordialement,<br>" +
                "l'équipe de l'ALOD");
        emailService.sendEmail(mess);

        confirmationTokenService.saveConfirmationToken(cft);
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

            SimpleMailMessage mess = new SimpleMailMessage();
            mess.setTo(user.getUsername());
            mess.setSubject("Réinitialisation du mot de passe");
            mess.setText("Bonjour,<br>" +
                    "Ceci est le <a href=https://" + serverName + "/adhesion/#/resetPassword/" + uuid + ">lien de renouvellement de votre mot de passe</a><br>" +
                    "Cordialement,<br>" +
                    "l'équipe de l'ALOD");
            emailService.sendEmail(mess);
            return cft;

    }

    public void changePassword(String token, String password) {
        ConfirmationToken confirmationToken= confirmationTokenService.findByToken(token);

            final User user = confirmationToken.getUser();
            user.setPassword( encoder.encode(password));
            userRepository.save(user);

            confirmationTokenService.deleteConfirmationToken(confirmationToken.getId());

    }

    public List<UserLite> getAllLite(){
        List<User> users = userRepository.findAll();
        List<UserLite> userLites = users.stream().map(user -> new UserLite(user)).collect(Collectors.toList());
        return userLites;
    }

    public void fillUsers(){
        try (BufferedReader br = new BufferedReader(new FileReader("./import_adherents.csv"))) {
            String line;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
            int lineNumber = 1;
            Random random = new Random();
            while ((line = br.readLine()) != null) {

                String[] values = line.split(";");
                String email = values[4].toLowerCase();
                if(!userRepository.existsByUsername(email)){
                    String password = random.toString();
                    User user = new User(email,encoder.encode(password));
                    Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                            .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    user.getRoles().add(userRole);

                    user = userRepository.save(user);
                    if(lineNumber == 1){
                        grantUser(ERole.ROLE_ADMIN, user);
                    }

                    if("alodbasket@free.fr".equalsIgnoreCase(email) || "alod.amicale@gmail.com".equalsIgnoreCase(email)){
                        grantUser(ERole.ROLE_SECRETAIRE, user);
                    }


                    if(LocalDate.parse(values[2], formatter).isAfter(LocalDate.of(2005,01,01))){
                        Adherent adherent = new Adherent();
                        adherent.setReferent(true);
                        adherent.setMineur(false);
                        adherent.setCompletAdhesion(false);
                        adherent.setCompletReferent(false);
                        adherent.setAdresseReferent(false);
                        adherent.setAdresse(values[7]+" "+values[8]+" "+values[9]);

                        adherent.setEmailReferent(false);
                        adherent.setEmail(email);

                        adherent.setTelephoneReferent(false);
                        adherent.setTelephone(values[5]);

                        adherent.getAccords().add(new Accord("RGPD"));
                        adherent.getAccords().add(new Accord("Droit Image"));

                        adherent.setUser(user);

                        adherent = adherentServices.save(adherent,null);


                        Adherent adherentMineur = new Adherent();

                        adherentMineur.setNom(values[0].toUpperCase());
                        adherentMineur.setPrenom(values[1].toLowerCase().substring(0, 1).toUpperCase()+values[1].toLowerCase().substring(1));
                        adherentMineur.setGenre(values[3]);

                        adherentMineur.setReferent(false);
                        adherentMineur.setMineur(true);
                        adherentMineur.setCompletAdhesion(false);
                        adherentMineur.setCompletReferent(false);

                        adherentMineur.setNaissance(LocalDate.parse(values[2], formatter));
                        adherentMineur.setLieuNaissance(values[6]);

                        adherentMineur.setAdresseReferent(true);
                        adherentMineur.setEmailReferent(true);
                        adherentMineur.setTelephoneReferent(true);

                        adherentMineur.getAccords().add(new Accord("RGPD"));
                        adherentMineur.getAccords().add(new Accord("Droit Image"));

                        adherentServices.save(adherentMineur,adherent.getTribu().getId());
                    }else {


                        Adherent adherent = new Adherent();
                        adherent.setNom(values[0].toUpperCase());
                        adherent.setPrenom(values[1].toLowerCase().substring(0, 1).toUpperCase()+values[1].toLowerCase().substring(1));
                        adherent.setGenre(values[3]);
                        adherent.setReferent(true);
                        adherent.setMineur(false);
                        adherent.setCompletAdhesion(false);
                        adherent.setCompletReferent(false);

                        adherent.setNaissance(LocalDate.parse(values[2], formatter));
                        adherent.setLieuNaissance(values[6]);

                        adherent.setAdresseReferent(false);
                        adherent.setAdresse(values[7]+" "+values[8]+" "+values[9]);

                        adherent.setEmailReferent(false);
                        adherent.setEmail(email);

                        adherent.setTelephoneReferent(false);
                        adherent.setTelephone(values[5]);

                        adherent.getAccords().add(new Accord("RGPD"));
                        adherent.getAccords().add(new Accord("Droit Image"));

                        adherent.setUser(user);

                        adherentServices.save(adherent,null);

                    }

                }else{
                    User user = findByEmail(email);

                    Adherent adherent = new Adherent();
                    adherent.setNom(values[0]);
                    adherent.setPrenom(values[1]);
                    adherent.setGenre(values[3]);
                    adherent.setReferent(false);
                    if(LocalDate.parse(values[2], formatter).isAfter(LocalDate.of(2005,01,01))){
                        adherent.setMineur(true);
                    }else {
                        adherent.setMineur(false);
                    }
                    adherent.setCompletAdhesion(false);
                    adherent.setCompletReferent(false);

                    adherent.setNaissance(LocalDate.parse(values[2], formatter));
                    adherent.setLieuNaissance(values[6]);

                    adherent.setAdresseReferent(true);
                    adherent.setEmailReferent(true);
                    adherent.setTelephoneReferent(true);


                    adherent.getAccords().add(new Accord("RGPD"));
                    adherent.getAccords().add(new Accord("Droit Image"));

                    adherentServices.save(adherent,user.getAdherent().getTribu().getId());

                }

                lineNumber++;
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

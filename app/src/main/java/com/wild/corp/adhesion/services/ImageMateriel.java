package com.wild.corp.adhesion.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/** Photo normalisée pour les cartes, stockée avec le matériel sous forme de data URL. */
final class ImageMateriel {
    private static final int MAX_OCTETS = 5 * 1024 * 1024;

    private ImageMateriel() {}

    static String redimensionner(String image) {
        if (image == null || image.isEmpty()) return null;
        if (image.length() > 4 * ((MAX_OCTETS + 2) / 3) + 64)
            throw invalide("La photo ne doit pas dépasser 5 Mo");
        if (!image.startsWith("data:image/jpeg;base64,") && !image.startsWith("data:image/png;base64,"))
            throw invalide("Choisissez une photo JPEG ou PNG");
        try {
            byte[] donnees = Base64.getDecoder().decode(image.substring(image.indexOf(',') + 1));
            if (donnees.length > MAX_OCTETS) throw invalide("La photo ne doit pas dépasser 5 Mo");
            try (var entree = ImageIO.createImageInputStream(new ByteArrayInputStream(donnees))) {
                var lecteurs = ImageIO.getImageReaders(entree);
                if (!lecteurs.hasNext()) throw invalide("La photo est illisible");
                var lecteur = lecteurs.next();
                try {
                    lecteur.setInput(entree, true, true);
                    String format = lecteur.getFormatName();
                    if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
                        throw invalide("Choisissez une photo JPEG ou PNG");
                    int largeur = lecteur.getWidth(0), hauteur = lecteur.getHeight(0);
                    if (largeur < 1 || hauteur < 1 || (long) largeur * hauteur > 40_000_000)
                        throw invalide("La photo ne doit pas dépasser 40 mégapixels");
                    // Le décodage sous-échantillonné limite la mémoire des grandes photos.
                    var parametres = lecteur.getDefaultReadParam();
                    int pas = Math.max(1, Math.max(largeur / 640, hauteur / 480));
                    parametres.setSourceSubsampling(pas, pas, 0, 0);
                    BufferedImage source = lecteur.read(0, parametres);
                    double rapport = Math.min(1, Math.min(640.0 / largeur, 480.0 / hauteur));
                    BufferedImage cible = new BufferedImage(Math.max(1, (int) Math.round(largeur * rapport)),
                            Math.max(1, (int) Math.round(hauteur * rapport)), BufferedImage.TYPE_INT_RGB);
                    var dessin = cible.createGraphics();
                    try {
                        dessin.setColor(Color.WHITE);
                        dessin.fillRect(0, 0, cible.getWidth(), cible.getHeight());
                        dessin.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        dessin.drawImage(source, 0, 0, cible.getWidth(), cible.getHeight(), null);
                    } finally {
                        dessin.dispose();
                    }
                    var sortie = new ByteArrayOutputStream();
                    ImageIO.write(cible, "jpeg", sortie);
                    return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(sortie.toByteArray());
                } finally {
                    lecteur.dispose();
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw invalide("La photo est illisible");
        }
    }

    private static ResponseStatusException invalide(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}

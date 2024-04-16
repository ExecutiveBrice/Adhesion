package com.wild.corp.adhesion.services;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.Paiement;
import lombok.extern.slf4j.Slf4j;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.nio.file.FileSystems;
import java.time.LocalDate;
import java.util.*;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
public class PdfService {
    private final SpringTemplateEngine templateEngine;
    public PdfService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }
    private static ThreadLocal<ByteArrayOutputStream> BUFFERS = ThreadLocal
            .withInitial(() -> new ByteArrayOutputStream(8192));
    private static ThreadLocal<CharArrayWriter> STRING_WRITERS = ThreadLocal
            .withInitial(() -> new CharArrayWriter(8192));

    public byte[] generateSynthese(Adherent adherent) {

        Integer total = adherent.getAdhesions().stream().filter(Adhesion::isValide).flatMap(adhesion -> adhesion.getPaiements().stream().filter(paiement -> paiement != null))
                .map(Paiement::getMontant)
                .reduce(0, (t, t2) -> t + t2);

        Map<String, List<String>> cfmPrint = new HashMap<>();


        cfmPrint.put("enTete", List.of("Agrément SDJES-EPJE/2023-44-19 du 20 nov 2023<br/>Déclaré en Préfecture de Nantes N° W442004438<br/>SIRET 786 043 364 00011", ""));

        cfmPrint.put("titre", List.of("ATTESTATION"));

        List<String> corps = new ArrayList<>();
        corps.add("Je soussignée Morgane Maury, présidente de l’Amicale Laïque de l’Ouche Dinier de REZE");
        corps.add(" atteste que " + (adherent.getGenre().equals("Masculin")?"Monsieur ":"Madame ") + adherent.getPrenom() + " " + adherent.getNom() + " né"+(adherent.getGenre().equals("Masculin")?"":"e")+" le "+ adherent.getNaissance());
        if(adherent.getAdhesions().stream().filter(Adhesion::isValide).toList().size()>1) {
            corps.add("est inscrit"+ (adherent.getGenre().equals("Masculin")?"":"e ")+" aux cours :");
            adherent.getAdhesions().stream().filter(Adhesion::isValide).forEach(adhesion ->
                    corps.add("- " + adhesion.getActivite().getNom())
            );
        }else{
            corps.add("est inscrit"+ (adherent.getGenre().equals("Masculin")?"":"e ")+" au cours de "+ adherent.getAdhesions().stream().findFirst().get().getActivite().getNom());
        }
        corps.add("pour l'année scolaire 2024/2025");
        corps.add("et est à jour de sa cotisation qui s’élève à " + total + "€");
        cfmPrint.put("corps", corps);

        cfmPrint.put("signature", List.of("Fait à Rezé le "+ LocalDate.now()));


        byte[] response;
        try {
            response = generatePdf("attestationTemplate", cfmPrint);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }



    public byte[] generatePdf(String template, Map<String, List<String>> contextVariables) throws IOException {
        long begin = System.currentTimeMillis();
        Context context = new Context();
        context.setLocale(Locale.FRANCE);
        contextVariables.forEach(context::setVariable);

        CharArrayWriter templateOutWriter = STRING_WRITERS.get();
        templateOutWriter.reset();

        templateEngine.process(template, context, templateOutWriter);

        String mergedHtml = templateOutWriter.toString();

        ByteArrayOutputStream rendererOutputStream = BUFFERS.get();
        rendererOutputStream.reset();

        openhtmltopdf(mergedHtml, rendererOutputStream);

        templateOutWriter.close();
        log.info("PDF rendered in {} ms", (System.currentTimeMillis() - begin));
        return rendererOutputStream.toByteArray();
    }

    private void openhtmltopdf(String html, ByteArrayOutputStream buffer) throws IOException {

        String baseUrl = FileSystems.getDefault()
                .getPath("app/src/main/resources/")
                .toUri().toURL().toString();
        System.out.println(baseUrl);
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useDefaultPageSize(210, 297, BaseRendererBuilder.PageSizeUnits.MM);
        // Load SVG support plugin
        builder.useSVGDrawer(new BatikSVGDrawer());
        // Load the fonts
        loadAllFonts(builder);
        // html cleaner
        String cleanedHtml = cleanHtml(html);
        // build the pdf doc
        builder.withHtmlContent(cleanedHtml, baseUrl);
        builder.toStream(buffer);
        builder.run();
    }

    private String cleanHtml(String html) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties properties = cleaner.getProperties();
        properties.setUseEmptyElementTags(true);
        properties.setOmitUnknownTags(true);
        TagNode tagNode = cleaner.clean(html);
        try {
            new PrettyXmlSerializer(properties).writeToStream(tagNode,baos,"UTF-8");
            return baos.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                baos.close();
            } catch (IOException e) {
                log.error("Unable to clean html", e);
            }
        }
    }

    private void loadAllFonts(PdfRendererBuilder builder) {
        final String fontUrl = "/fonts";
        File folder;
        try {
            folder = ResourceUtils.getFile(fontUrl);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null && listOfFiles.length > 0) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    String fileName = listOfFiles[i].getName();
                    String fontName = fileName.replace(".ttf", "").replace("_", " ");
                    try {
                        builder.useFont(new File(ResourceUtils.getFile(fontUrl) + "/" + fileName), fontName);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}

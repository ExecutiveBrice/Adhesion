package com.gestion.emails.services;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MailingList {

    private final List<List<String>> listing = new ArrayList<>();

    private static MailingList instance;

    public static MailingList getInstance() {
        if (instance == null) {
            instance = new MailingList();
        }
        return instance;
    }

    MailingList(){
        try (BufferedReader br = new BufferedReader(new FileReader("./listing.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                listing.add(Arrays.asList(values));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<List<String>> getListing() {
        return listing;
    }
}

package com.t24extractor.utils;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * @Author: Rami Bahrouni
 * @Email: rami.bahrouni@banquezitouna.com
 */

/**
 * Avec cette classe, on peurt renommer les noms des tables T24 pour les utiliser lors de requetage avec les tables TAFJ_VOC & FILE.CONTROL
 */
public class Renamer {
    /**
     *
     * @param t24Name Le nom de la table T24 enregistre dans la table batch_config
     * @return nouveau format du nom de la table
     * Ex: ACCOUNT$HIS ==> ACCOUNT
     */
    public String ssRenamer(String t24Name){
        String t24name =  Optional.ofNullable(t24Name)
                .orElseThrow(() ->
                        new NoSuchElementException
                                (String.format("Veuillez verifier que la valeur %s est correcte", t24Name)));
        if (t24name.contains("$")){
            t24name = t24name.substring(0, t24Name.indexOf("$"));
        }
        return t24name;
    }

    /**
     *
     * @param t24Name Le nom de la table T24 enregistre dans la table batch_config
     * @return nouveau format du nom de la table
     * Ex: ACCOUNT$HIS ==> ACCOUNT#HIS, FUNDS.TRANSFER ==> FUNDS_TRANSFER
     */
    public String biRenamer(String t24Name){
        String t24name =  Optional.ofNullable(t24Name)
                .orElseThrow(() ->
                        new NoSuchElementException
                                (String.format("Veuillez verifier que la valeur %s est correcte", t24Name))).replaceAll("\\.", "_").replace("$", "#");
        return t24name.contains("\\$")?t24name.substring(0, t24Name.indexOf("\\$")):t24name;
    }
}

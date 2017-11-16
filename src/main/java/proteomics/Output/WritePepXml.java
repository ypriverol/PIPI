package proteomics.Output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proteomics.PIPI;
import proteomics.TheoSeq.MassTool;
import proteomics.Types.*;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class WritePepXml {

    private static final Logger logger = LoggerFactory.getLogger(WritePepXml.class);

    private final String outputPath;
    private final String baseName;
    private final String rawDataType;
    private final Map<String, String> parameterMap;

    public WritePepXml(Map<Integer, FinalResultEntry> scanFinalResultMap, String outputPath, String spectraName, Map<String, String> parameterMap, Map<Character, Float> massTable, Map<Integer, PercolatorEntry> percolatorResultMap, Map<String, Peptide0> peptide0Map) {
        this.outputPath = outputPath;
        int tempIdx = spectraName.lastIndexOf('.');
        baseName = spectraName.substring(0, tempIdx);
        rawDataType = spectraName.substring(tempIdx);
        this.parameterMap = parameterMap;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))){
            writer.write(pepxmlHeader(massTable));
            for (FinalResultEntry finalResultEntry : scanFinalResultMap.values()) {
                if (percolatorResultMap.containsKey(finalResultEntry.getScanNum())) {
                    Peptide peptide = finalResultEntry.getPeptideSet().first();
                    Peptide0 peptide0 = peptide0Map.get(peptide.getPTMFreeSeq());
                    StringBuilder proteinIdStr = new StringBuilder();
                    for (String proteinId : peptide0.proteins) {
                        proteinIdStr.append(proteinId);
                        proteinIdStr.append(";");
                    }
                    float expMass = finalResultEntry.getCharge() * (finalResultEntry.getPrecursorMz() - 1.00727646688f);
                    String ptmDeltaScore;
                    if (finalResultEntry.getPtmPatterns().containsKey(peptide.getPTMFreeSeq())) {
                        TreeSet<Peptide> tempTreeSet = finalResultEntry.getPtmPatterns().get(peptide.getPTMFreeSeq());
                        if (tempTreeSet.size() > 1) {
                            Iterator<Peptide> temp = tempTreeSet.iterator();
                            ptmDeltaScore = String.valueOf(temp.next().getScore() - temp.next().getScore());
                        } else {
                            ptmDeltaScore = String.valueOf(tempTreeSet.first().getScore());
                        }
                    } else {
                        ptmDeltaScore = "-";
                    }
                    PercolatorEntry percolatorEntry = percolatorResultMap.get(finalResultEntry.getScanNum());

                    writer.write(String.format(Locale.US,
                            "\t\t<spectrum_query spectrum=\"%d\" start_scan=\"%d\" end_scan=\"%d\" precursor_neutral_mass=\"%f\" assumed_charge=\"%d\" index=\"%d\">\r\n" +
                                    "\t\t\t<search_result>\r\n" +
                                    "\t\t\t\t<search_hit hit_rank=\"1\" peptide=\"%s\" peptide_prev_aa=\"%c\" peptide_next_aa=\"%c\" protein=\"%s\" num_tot_proteins=\"%d\" num_matched_ions=\"%d\" tot_num_ions=\"%d\" calc_neutral_pep_mass=\"%f\" massdiff=\"%f\" num_tol_term=\"2\">\r\n" +
                                    "\t\t\t\t\t<search_score name=\"score\" value=\"%f\"/>\r\n" +
                                    "\t\t\t\t\t<search_score name=\"ptm_delta_score\" value=\"%s\"/>\r\n" +
                                    "\t\t\t\t\t<search_score name=\"ptm_supporting_peak_frac\" value=\"%f\"/>\r\n" +
                                    "\t\t\t\t\t<search_score name=\"percolator_score\" value=\"%f\"/>\r\n" +
                                    "\t\t\t\t\t<search_score name=\"percolator_error_prob\" value=\"%s\"/>\r\n" +
                                    "\t\t\t\t\t<search_score name=\"q_value\" value=\"%s\"/>\r\n" +
                                    "\t\t\t\t\t<search_score name=\"labeling\" value=\"%s\"/>\r\n", finalResultEntry.getScanNum(), finalResultEntry.getScanNum(), finalResultEntry.getScanNum(), expMass, finalResultEntry.getCharge(), finalResultEntry.getScanNum(), peptide.getPTMFreeSeq().replaceAll("[nc]", ""), peptide0.leftFlank, peptide0.rightFlank, proteinIdStr.toString(), peptide0.proteins.size(), peptide.getMatchedPeakNum(), (peptide.length() - 2) * 2 * Math.max(1, finalResultEntry.getCharge() - 1), peptide.getTheoMass(), PIPI.getMassDiff(expMass, peptide.getTheoMass(), MassTool.C13_DIFF), peptide.getScore(), ptmDeltaScore, peptide.getPtmSupportingPeakFrac(), percolatorEntry.percolatorScore, percolatorEntry.PEP, percolatorEntry.qValue, finalResultEntry.getLabeling()));

                    if (peptide.hasVarPTM()) {
                        PositionDeltaMassMap ptmMap = peptide.getVarPTMs();
                        if (ptmMap.containsKey(new Coordinate(0, 1)) && ptmMap.containsKey(new Coordinate(ptmMap.peptideLength - 1, ptmMap.peptideLength))) {
                            writer.write(String.format(Locale.US, "\t\t\t\t\t<modification_info modified_peptide=\"%s\" mod_nterm_mass=\"%f\" mod_cterm_mass=\"%f\">\r\n", peptide.getVarPtmContainingSeq(), ptmMap.get(new Coordinate(0, 1)) + MassTool.PROTON, ptmMap.get(new Coordinate(ptmMap.peptideLength - 1, ptmMap.peptideLength))));
                        } else if (ptmMap.containsKey(new Coordinate(0, 1))) {
                            writer.write(String.format(Locale.US, "\t\t\t\t\t<modification_info modified_peptide=\"%s\" mod_nterm_mass=\"%f\">\r\n", peptide.getVarPtmContainingSeq(), ptmMap.get(new Coordinate(0, 1)) + MassTool.PROTON));
                        } else if (ptmMap.containsKey(new Coordinate(ptmMap.peptideLength - 1, ptmMap.peptideLength))) {
                            writer.write(String.format(Locale.US, "\t\t\t\t\t<modification_info modified_peptide=\"%s\" mod_cterm_mass=\"%f\">\r\n", peptide.getVarPtmContainingSeq(), ptmMap.get(new Coordinate(ptmMap.peptideLength - 1, ptmMap.peptideLength))));
                        } else {
                            writer.write(String.format(Locale.US, "\t\t\t\t\t<modification_info modified_peptide=\"%s\">\r\n", peptide.getVarPtmContainingSeq()));
                        }
                        for (Coordinate co : ptmMap.keySet()) {
                            if (co.x != 0 && co.x != ptmMap.peptideLength - 1) {
                                writer.write(String.format(Locale.US, "\t\t\t\t\t\t<mod_aminoacid_mass position=\"%d\" mass=\"%f\"/>\r\n", co.x, massTable.get(peptide.getPTMFreeSeq().charAt(co.x)) + ptmMap.get(co)));
                            }
                        }
                        writer.write("\t\t\t\t\t</modification_info>\r\n");
                    }
                    writer.write("\t\t\t\t</search_hit>\r\n" +
                            "\t\t\t</search_result>\r\n" +
                            " \t\t</spectrum_query>\r\n");
                }
            }
            writer.write("\t</msms_run_summary>\r\n" +
                            "</msms_pipeline_analysis>\r\n");
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
            System.exit(1);
        }
    }

    private String pepxmlHeader(Map<Character, Float> massTable) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        StringBuilder header = new StringBuilder();
        header.append(String.format(Locale.US,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
                "<msms_pipeline_analysis date=\"%s\" xmlns=\"http://regis-web.systemsbiology.net/pepXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://sashimi.sourceforge.net/schema_revision/pepXML/pepXML_v117.xsd\" summary_xml=\"%s\">\r\n" +
                "\t<msms_run_summary base_name=\"%s\" raw_data_type=\"%s\" raw_data=\"%s\">\r\n" +
                "\t\t<sample_enzyme name=\"%s\">\r\n" +
                "\t\t\t<specificity cut=\"%s\" no_cut=\"%s\" sense=\"%c\"/>\r\n" +
                "\t\t</sample_enzyme>\r\n" +
                "\t\t<search_summary base_name=\"%s\" search_engine=\"PIPI\" search_engine_version=\"%s\" precursor_mass_type=\"monoisotopic\" fragment_mass_type=\"monoisotopic\" search_id=\"1\">\r\n" +
                "\t\t\t<search_database local_path=\"%s\" type=\"AA\"/>\r\n" +
                "\t\t\t<enzymatic_search_constraint enzyme=\"%s\" max_num_internal_cleavages=\"%s\" min_number_termini=\"2\"/>\r\n", dateFormat.format(date), outputPath, baseName, rawDataType, rawDataType, parameterMap.get("enzyme_name"), parameterMap.get("cleavage_site"), parameterMap.get("protection_site"), parameterMap.get("cleavage_from_c_term").contentEquals("1") ? 'C' : 'N', baseName, PIPI.versionStr, parameterMap.get("db"), parameterMap.get("enzyme_name"), parameterMap.get("missed_cleavage")));
        for (String k : parameterMap.keySet()) {
            if (k.startsWith("mod") && !parameterMap.get(k).trim().startsWith("0.0")) {
                String[] parts = parameterMap.get(k).trim().split("@");
                header.append(String.format(Locale.US, "\t\t\t<aminoacid_modification aminoacid=\"%c\" massdiff=\"%s\" mass=\"%f\" variable=\"Y\"/>\r\n", parts[1].charAt(0), parts[0].trim(), massTable.get(parts[1].charAt(0)) + Float.valueOf(parts[0])));
            } else if (k.startsWith("Nterm") && Math.abs(Float.valueOf(parameterMap.get(k).trim())) > 0.5) {
                String[] parts = parameterMap.get(k).trim().split(",");
                for (String part : parts) {
                    header.append(String.format(Locale.US, "\t\t\t<terminal_modification terminus=\"N\" massdiff=\"%s\" mass=\"%f\" variable=\"Y\" protein_terminus=\"N\"/>\r\n", part.trim(), MassTool.PROTON + Float.valueOf(parts[0])));
                }
            } else if (k.startsWith("Cterm") && Math.abs(Float.valueOf(parameterMap.get(k).trim())) > 0.5) {
                String[] parts = parameterMap.get(k).trim().split(",");
                for (String part : parts) {
                    header.append(String.format(Locale.US, "\t\t\t<terminal_modification terminus=\"C\" massdiff=\"%s\" mass=\"%s\" variable=\"Y\" protein_terminus=\"N\"/>\r\n", part.trim(), part.trim()));
                }
            } else if (k.matches("[A-Z]") && Math.abs(Float.valueOf(parameterMap.get(k).trim())) > 0.5) {
                header.append(String.format(Locale.US, "\t\t\t<aminoacid_modification aminoacid=\"%c\" massdiff=\"%s\" mass=\"%f\" variable=\"N\"/>\r\n", k.charAt(0), parameterMap.get(k).trim(), massTable.get(k.charAt(0)) + Float.valueOf(parameterMap.get(k))));
            } else if (k.contentEquals("n") && Math.abs(Float.valueOf(parameterMap.get(k).trim())) > 0.5) {
                header.append(String.format(Locale.US, "\t\t\t<terminal_modification terminus=\"N\" massdiff=\"%s\" mass=\"%s\" variable=\"N\" protein_terminus=\"N\"/>\r\n", parameterMap.get(k).trim(), MassTool.PROTON + Float.valueOf(parameterMap.get(k).trim())));
            } else if (k.contentEquals("c") && Math.abs(Float.valueOf(parameterMap.get(k).trim())) > 0.5) {
                header.append(String.format(Locale.US, "\t\t\t<terminal_modification terminus=\"C\" massdiff=\"%s\" mass=\"%s\" variable=\"N\" protein_terminus=\"N\"/>\r\n", parameterMap.get(k).trim(), Float.valueOf(parameterMap.get(k).trim())));
            }
        }
        for (String k : parameterMap.keySet()) {
            if (!k.startsWith("mod") && !k.startsWith("Nterm") && !k.startsWith("Cterm")) {
                header.append(String.format(Locale.US, "\t\t\t<parameter name=\"%s\" value=\"%s\"/>\r\n", k, parameterMap.get(k).trim()));
            }
        }
        header.append("\t\t</search_summary>\r\n");
        return header.toString();
    }
}
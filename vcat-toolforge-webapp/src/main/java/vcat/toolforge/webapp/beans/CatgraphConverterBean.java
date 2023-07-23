package vcat.toolforge.webapp.beans;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import vcat.toolforge.webapp.CatgraphConverter;
import vcat.toolforge.webapp.Messages;

import java.util.*;

@Named("catgraphConverter")
@ViewScoped
public class CatgraphConverterBean {

    private Messages messages;

    @Getter
    private Map<String, String[]> inputParameters;

    @Getter
    @Setter
    private String inputUrl;

    @Getter
    private Map<String, String[]> outputParameters;

    @Getter
    private String outputUrl;

    @Getter
    private boolean hasResult;

    @PostConstruct
    void init() {
        final var requestParameters = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String lang = requestParameters.get("lang");
        if (lang == null) {
            if (messages == null) {
                messages = new Messages("en");
            }
        } else {
            messages = new Messages(lang);
        }
    }

    public void convert() {
        final int queryStart = inputUrl.indexOf('?');
        final String urlParameters = inputUrl.substring(queryStart + 1);
        inputParameters = new TreeMap<>();
        for (String nameValueString : urlParameters.split("&")) {
            final String[] split = nameValueString.split("=");
            final String name = split[0];
            String value = null;
            if (split.length > 1) {
                value = split[1];
            }
            if (inputParameters.containsKey(name)) {
                final String[] oldValues = inputParameters.get(name);
                final String[] newValues = Arrays.copyOf(oldValues, oldValues.length + 1);
                newValues[oldValues.length] = value;
                inputParameters.put(name, newValues);
            } else {
                inputParameters.put(name, new String[]{value});
            }
        }

        outputParameters = CatgraphConverter.convertParameters(inputParameters);

        // Build URL and list for output, only with known parameters
        final String[] sortList = new String[]{"wiki", "category", "title", "ns", "rel", "depth", "limit",
                "showhidden", "algorithm", "format", "links"};
        final List<String> outputParameterList = new ArrayList<>();
        for (String key : sortList) {
            final String[] values = outputParameters.get(key);
            if (values != null) {
                for (String value : values) {
                    outputParameterList.add(key + '=' + value);
                }
            }
        }
        outputUrl = "http://tools.wmflabs.org/vcat/render?"
                + StringUtils.join(outputParameterList, '&');
        hasResult = true;
    }

    public String msg(String key) {
        return messages.getCatgraphConverterString(key);
    }

}

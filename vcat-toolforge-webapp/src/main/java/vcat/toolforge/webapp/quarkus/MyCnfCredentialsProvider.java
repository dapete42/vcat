package vcat.toolforge.webapp.quarkus;

import io.quarkus.arc.Unremovable;
import io.quarkus.credentials.CredentialsProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import vcat.VCatException;
import vcat.toolforge.webapp.MyCnfConfig;

import java.util.Map;

@Named("mycnf")
@ApplicationScoped
@Unremovable
public class MyCnfCredentialsProvider implements CredentialsProvider {

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        MyCnfConfig config = new MyCnfConfig();
        try {
            config.readFromMyCnf();
        } catch (VCatException e) {
            throw new RuntimeException(e);
        }
        return Map.of(
                USER_PROPERTY_NAME, config.getUser(),
                PASSWORD_PROPERTY_NAME, config.getPassword()
        );
    }

}

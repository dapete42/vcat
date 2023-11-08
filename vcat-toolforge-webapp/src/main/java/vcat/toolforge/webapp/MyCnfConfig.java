package vcat.toolforge.webapp;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import vcat.VCatException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Slf4j
public class MyCnfConfig {

    private static final String MY_CNF = "replica.my.cnf";

    private String user;

    private String password;

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public boolean readFromMyCnf() throws VCatException {

        String userHome = System.getProperty("user.home");
        String toolDataDir = System.getenv("TOOL_DATA_DIR");

        // Primary: from $HOME
        Path myCnfFile = Paths.get(userHome, MY_CNF);
        if (Files.exists(myCnfFile)) {
            LOG.info("Using {} from {}", MY_CNF, myCnfFile);
        } else {
            // Secondary: from $TOOL_DATA_DIR (within container built by Build Service)
            if (toolDataDir != null) {
                myCnfFile = Paths.get(toolDataDir, MY_CNF);
                if (Files.exists(myCnfFile)) {
                    LOG.info("Using {} from {}", MY_CNF, myCnfFile);
                } else {
                    throw new VCatException(
                            MessageFormatter.format("{} not found in home or $TOOL_DATA_DIR", MY_CNF).getMessage());
                }
            } else {
                throw new VCatException(
                        MessageFormatter.format("{} not found in home", MY_CNF).getMessage());
            }
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(myCnfFile);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            properties.load(bufferedReader);
        } catch (IOException e) {
            throw new VCatException(
                    MessageFormatter.format("Error reading file '{}'", myCnfFile.toAbsolutePath()).getMessage(), e);
        }

        int errors = 0;

        user = properties.getProperty("user");
        if (user == null || user.isEmpty()) {
            LOG.error("Property '{}' missing or empty", "user");
            errors++;
        }
        if (user != null && user.startsWith("'") && user.endsWith("'")) {
            user = user.substring(1, user.length() - 1);
        }

        password = properties.getProperty("password");
        if (password == null || password.isEmpty()) {
            LOG.error("Property '{}' missing or empty", "password");
            errors++;
        }
        if (password != null && password.startsWith("'") && password.endsWith("'")) {
            password = password.substring(1, password.length() - 1);
        }

        return errors == 0;
    }
}

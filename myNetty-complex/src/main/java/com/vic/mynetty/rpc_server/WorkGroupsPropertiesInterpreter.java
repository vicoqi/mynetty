package com.vic.mynetty.rpc_server;

import com.vic.mynetty.netty_server.config.WorkGroupConfig;
import com.vic.mynetty.utils.PrintUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class WorkGroupsPropertiesInterpreter {

    private static final Pattern ROOT_PATTERN = Pattern.compile("(comm\\.)?(\\w+)");
    private static final Pattern WORK_GROUP_PATTERN = Pattern.compile("(comm\\.)?workGroup\\.(\\w+)\\.qryStr");

    public static List<WorkGroupConfig> interpret(String filePath, String root, boolean relativePath) {
        Properties properties = new Properties();
        String realPath = filePath;
        try {
            if (relativePath) {
                File runningJarFile = new File(
                    WorkGroupsPropertiesInterpreter.class.getProtectionDomain().getCodeSource().getLocation().toURI()
                        .getPath())
                    .getParentFile().getParentFile();
                realPath = runningJarFile.getPath().concat(File.separator).concat(filePath);
            }
            File propFile = new File(realPath);
            properties.load(new FileInputStream(propFile));
            return interpret(properties, root);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("property file not found|file=[%s]", filePath), e);
        }
    }

    public static List<WorkGroupConfig> interpret(String propertiesFilePath, String root) {
        Properties properties = new Properties();
        try {
            String name = "conf/" + propertiesFilePath;
            File file = new File(name);
            InputStream inputStream = null;
            if (file.exists()) {
                inputStream = new FileInputStream(file);
            } else {
                inputStream = WorkGroupsPropertiesInterpreter.class
                    .getClassLoader()
                    .getResourceAsStream(name);
            }
            properties.load(inputStream);
            return interpret(properties, root);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                String.format("property file not found in classpath|file=[%s]", propertiesFilePath), e);
        }
    }

    public static List<WorkGroupConfig> interpret(Properties properties, String root) {
        String rootValue = properties.getProperty(root);
        String[] workGroupNames = rootValue.split(",");
        List<WorkGroupConfig> workGroupConfigs = new ArrayList<WorkGroupConfig>(workGroupNames.length);
        for (Object key : properties.keySet()) {
            String keyStr = (String)key;
            String value = properties.getProperty(keyStr);
            Matcher workGroupMatcher = WORK_GROUP_PATTERN.matcher(keyStr);
            if (workGroupMatcher.matches() && !keyStr.equals(root)) {
                workGroupMatcher.reset();
                if (workGroupMatcher.find()) {
                    String workGroupName;
                    if (workGroupMatcher.groupCount() == 1) {
                        workGroupName = workGroupMatcher.group(1);
                    } else {
                        workGroupName = workGroupMatcher.group(2);
                    }
                    WorkGroupConfig workGroupConfig = WorkGroupConfig.URIInterpreter.interpret(workGroupName, value);
                    if (ArrayUtils.contains(workGroupNames, workGroupName)) {
                        workGroupConfigs.add(workGroupConfig);
                    } else {
                        log.warn("ignore wrongly configured session|name=[{}]|expectedSessions={}",
                            workGroupName, PrintUtil.print(workGroupNames));
                    }
                }
            }
        }
        return workGroupConfigs;
    }
}

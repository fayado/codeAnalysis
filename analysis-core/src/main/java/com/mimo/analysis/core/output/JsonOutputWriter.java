package com.mimo.analysis.core.output;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mimo.analysis.core.model.*;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class JsonOutputWriter implements OutputWriter {

    private static final int TOP_LIMIT = 20;

    @Override
    public void write(AnalysisReport report, Writer output) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject root = new JsonObject();

        // metadata
        JsonObject metadata = new JsonObject();
        metadata.addProperty("generatedAt", report.getGeneratedAt().toString());
        metadata.addProperty("analysisMode", report.getAnalysisMode());
        metadata.addProperty("totalClasses", report.getTotalClassCount());
        metadata.addProperty("totalReferences", report.getTotalReferenceCount());
        root.add("metadata", metadata);

        // packages
        JsonObject packages = new JsonObject();
        for (Map.Entry<String, PackageStats> entry : report.getPackageStats().entrySet()) {
            PackageStats stats = entry.getValue();
            JsonObject pkgJson = new JsonObject();
            pkgJson.addProperty("classCount", stats.getClassCount());
            pkgJson.addProperty("interfaceCount", stats.getInterfaceCount());
            pkgJson.addProperty("abstractClassCount", stats.getAbstractClassCount());
            pkgJson.addProperty("annotationCount", stats.getAnnotationCount());
            pkgJson.addProperty("enumCount", stats.getEnumCount());
            pkgJson.addProperty("springComponentCount", stats.getSpringComponentCount());
            JsonArray classNames = new JsonArray();
            for (String name : stats.getClassNames()) {
                classNames.add(name);
            }
            pkgJson.add("classes", classNames);
            packages.add(entry.getKey(), pkgJson);
        }
        root.add("packages", packages);

        // classes
        JsonArray classesArr = new JsonArray();
        for (ClassInfo info : report.getAllClasses()) {
            JsonObject cls = new JsonObject();
            cls.addProperty("name", info.getClassName());
            cls.addProperty("simpleName", info.getSimpleName());
            cls.addProperty("package", info.getPackageName());
            cls.addProperty("jarSource", info.getJarSource());
            cls.addProperty("origin", info.getOrigin().name());
            cls.addProperty("isInterface", info.isInterface());
            cls.addProperty("isAbstract", info.isAbstract());
            cls.addProperty("isAnnotation", info.isAnnotation());
            cls.addProperty("isEnum", info.isEnum());
            cls.addProperty("isSpringComponent", info.isSpringComponent());
            cls.addProperty("inboundReferenceCount", report.getReferencesTo(info.getClassName()).size());
            cls.addProperty("outboundReferenceCount", report.getReferencesFrom(info.getClassName()).size());
            classesArr.add(cls);
        }
        root.add("classes", classesArr);

        // references
        JsonArray refsArr = new JsonArray();
        for (ClassReference ref : report.getAllReferences()) {
            JsonObject refJson = new JsonObject();
            refJson.addProperty("from", ref.getFromClass());
            refJson.addProperty("to", ref.getToClass());
            refJson.addProperty("type", ref.getType().name());
            refsArr.add(refJson);
        }
        root.add("references", refsArr);

        // statistics
        JsonObject stats = new JsonObject();

        JsonArray topReferenced = new JsonArray();
        for (Map.Entry<String, Integer> entry : report.getTopReferencedClasses(TOP_LIMIT)) {
            JsonObject item = new JsonObject();
            item.addProperty("class", entry.getKey());
            item.addProperty("count", entry.getValue());
            topReferenced.add(item);
        }
        stats.add("topReferencedClasses", topReferenced);

        JsonArray topPackages = new JsonArray();
        for (Map.Entry<String, Integer> entry : report.getTopPackages(TOP_LIMIT)) {
            JsonObject item = new JsonObject();
            item.addProperty("package", entry.getKey());
            item.addProperty("classCount", entry.getValue());
            topPackages.add(item);
        }
        stats.add("topPackages", topPackages);

        root.add("statistics", stats);

        gson.toJson(root, output);
        output.flush();
    }
}

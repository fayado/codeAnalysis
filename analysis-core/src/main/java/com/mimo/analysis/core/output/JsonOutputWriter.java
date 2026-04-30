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

/**
 * JSON 格式输出器，将分析报告以结构化的 JSON 格式输出。
 *
 * 输出的 JSON 结构包含以下部分：
 * - metadata：报告元数据（生成时间、分析模式、类总数、引用总数）
 * - packages：按包名分组的统计信息（类数、接口数、抽象类数、注解数、枚举数、Spring 组件数）
 * - classes：所有类的详细信息（名称、包名、JAR 来源、来源类型、类型标志、引用计数）
 * - references：所有引用关系（来源类、目标类、引用类型）
 * - statistics：统计摘要（被引用次数 Top N 类、类数量 Top N 包）
 *
 * 使用 Gson 库进行 JSON 序列化（而非 Jackson），避免循环依赖——
 * 因为 Jackson 类本身也在分析范围内。
 */
public class JsonOutputWriter implements OutputWriter {

    /** 统计摘要中 Top N 的默认限制数量 */
    private static final int TOP_LIMIT = 20;

    /**
     * 将分析报告以 JSON 格式写入输出流。
     *
     * @param report 分析报告
     * @param output 输出目标
     * @throws IOException 写入错误时抛出
     */
    @Override
    public void write(AnalysisReport report, Writer output) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject root = new JsonObject();

        // ========== 元数据部分 ==========
        JsonObject metadata = new JsonObject();
        metadata.addProperty("generatedAt", report.getGeneratedAt().toString());
        metadata.addProperty("analysisMode", report.getAnalysisMode());
        metadata.addProperty("totalClasses", report.getTotalClassCount());
        metadata.addProperty("totalReferences", report.getTotalReferenceCount());
        root.add("metadata", metadata);

        // ========== 包统计部分 ==========
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
            // 包下所有类名列表
            JsonArray classNames = new JsonArray();
            for (String name : stats.getClassNames()) {
                classNames.add(name);
            }
            pkgJson.add("classes", classNames);
            packages.add(entry.getKey(), pkgJson);
        }
        root.add("packages", packages);

        // ========== 类详情部分 ==========
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
            // 入引用数：有多少类引用了当前类
            cls.addProperty("inboundReferenceCount", report.getReferencesTo(info.getClassName()).size());
            // 出引用数：当前类引用了多少其他类
            cls.addProperty("outboundReferenceCount", report.getReferencesFrom(info.getClassName()).size());
            classesArr.add(cls);
        }
        root.add("classes", classesArr);

        // ========== 引用关系部分 ==========
        JsonArray refsArr = new JsonArray();
        for (ClassReference ref : report.getAllReferences()) {
            JsonObject refJson = new JsonObject();
            refJson.addProperty("from", ref.getFromClass());
            refJson.addProperty("to", ref.getToClass());
            refJson.addProperty("type", ref.getType().name());
            refsArr.add(refJson);
        }
        root.add("references", refsArr);

        // ========== 统计摘要部分 ==========
        JsonObject stats = new JsonObject();

        // 被引用次数最多的 Top N 类
        JsonArray topReferenced = new JsonArray();
        for (Map.Entry<String, Integer> entry : report.getTopReferencedClasses(TOP_LIMIT)) {
            JsonObject item = new JsonObject();
            item.addProperty("class", entry.getKey());
            item.addProperty("count", entry.getValue());
            topReferenced.add(item);
        }
        stats.add("topReferencedClasses", topReferenced);

        // 类数量最多的 Top N 包
        JsonArray topPackages = new JsonArray();
        for (Map.Entry<String, Integer> entry : report.getTopPackages(TOP_LIMIT)) {
            JsonObject item = new JsonObject();
            item.addProperty("package", entry.getKey());
            item.addProperty("classCount", entry.getValue());
            topPackages.add(item);
        }
        stats.add("topPackages", topPackages);

        root.add("statistics", stats);

        // 输出 JSON 并刷新
        gson.toJson(root, output);
        output.flush();
    }
}

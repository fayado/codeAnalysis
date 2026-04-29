package com.mimo.analysis.core.model;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class AnalysisReport {

    private final Map<String, ClassInfo> classes;
    private final List<ClassReference> references;
    private final Set<String> uniqueReferenceKeys;
    private final Map<String, PackageStats> packageStats;
    private final Instant generatedAt;
    private final String analysisMode;

    public AnalysisReport(String analysisMode) {
        this.classes = new LinkedHashMap<>();
        this.references = new ArrayList<>();
        this.uniqueReferenceKeys = new HashSet<>();
        this.packageStats = new LinkedHashMap<>();
        this.generatedAt = Instant.now();
        this.analysisMode = analysisMode;
    }

    public void addClass(ClassInfo info) {
        String name = info.getClassName();
        if (!classes.containsKey(name)) {
            classes.put(name, info);
            addClassToPackageStats(info);
        }
    }

    public void addClasses(Collection<ClassInfo> infos) {
        for (ClassInfo info : infos) {
            addClass(info);
        }
    }

    public void addReference(ClassReference ref) {
        String key = ref.toKey();
        if (uniqueReferenceKeys.add(key)) {
            references.add(ref);
        }
    }

    public void addReferences(Collection<ClassReference> refs) {
        for (ClassReference ref : refs) {
            addReference(ref);
        }
    }

    public void merge(AnalysisReport other) {
        for (Map.Entry<String, ClassInfo> entry : other.classes.entrySet()) {
            String name = entry.getKey();
            ClassInfo otherInfo = entry.getValue();

            if (this.classes.containsKey(name)) {
                ClassInfo existing = this.classes.get(name);
                if (existing.getOrigin() != otherInfo.getOrigin()) {
                    this.classes.put(name, existing.toBuilder()
                            .origin(ClassOrigin.BOTH)
                            .build());
                }
            } else {
                this.classes.put(name, otherInfo);
                addClassToPackageStats(otherInfo);
            }
        }

        for (ClassReference ref : other.references) {
            addReference(ref);
        }
    }

    public Collection<ClassInfo> getAllClasses() {
        return Collections.unmodifiableCollection(classes.values());
    }

    public List<ClassReference> getAllReferences() {
        return Collections.unmodifiableList(references);
    }

    public Map<String, PackageStats> getPackageStats() {
        return Collections.unmodifiableMap(packageStats);
    }

    public List<ClassInfo> getClassesByPackage(String packageName) {
        return classes.values().stream()
                .filter(c -> c.getPackageName().equals(packageName))
                .collect(Collectors.toList());
    }

    public List<ClassReference> getReferencesFrom(String className) {
        return references.stream()
                .filter(r -> r.getFromClass().equals(className))
                .collect(Collectors.toList());
    }

    public List<ClassReference> getReferencesTo(String className) {
        return references.stream()
                .filter(r -> r.getToClass().equals(className))
                .collect(Collectors.toList());
    }

    public ClassInfo getClass(String className) {
        return classes.get(className);
    }

    public int getTotalClassCount() {
        return classes.size();
    }

    public int getTotalReferenceCount() {
        return references.size();
    }

    public List<Map.Entry<String, Integer>> getTopReferencedClasses(int limit) {
        Map<String, Integer> countMap = new HashMap<>();
        for (ClassReference ref : references) {
            countMap.merge(ref.getToClass(), 1, Integer::sum);
        }
        return countMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<String, Integer>> getTopPackages(int limit) {
        return packageStats.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getClassCount()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Instant getGeneratedAt() { return generatedAt; }
    public String getAnalysisMode() { return analysisMode; }

    private void addClassToPackageStats(ClassInfo info) {
        String pkg = info.getPackageName();
        PackageStats stats = packageStats.computeIfAbsent(pkg, PackageStats::new);
        stats.addClass(info);
    }
}

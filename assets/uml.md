@startuml
'Désactiver l'affichage en couleur des icônes de visibilité'
skinparam classAttributeIconSize 0

package code_metier {

    class Measure {
        - name: String
        ~ Measure(name: String)
        + getName(): String
    }

    enum DataType {
        RAW
        TREND
        SEASONNALITY
        NOISE
    }

    class Tag {
        - value: String
        + {static} PREPARATION: Tag
        ~ Tag(value: String)
        + toString(): String
    }

    class DataPoint {
        - timestamp: float
        - value: Float
        ~ DataPoint(timestamp: float, value: Float)
        + getTimestamp(): float
        + getValue(): Float
    }
    
    class Range<T> {
        - minimum: T
        - maximum: T
        + Range(minimum: T, maximum: T)
        + contains(value: T): boolean
        + getMinimum(): T
        + setMinimum(minimum: T)
        + getMaximum(): T
        + getMaximum(maximum: T)
    }

    class ExperimentManager {
        - loader: ExperimentDataLoader;
        - cleaner: ExperimentDataCleaner;
        - decomposer: ExperimentDataDecomposer;
        + ExperimentManager()
        + load(\n\tfile: File,\n\tprogressCallback: BiConsumer<Integer,Integer>,\n\tcompletionHandler: BiConsumer<List<Measure>,List<Tag>>\n)
        - clean(\n\tmeasure: Measure,\n\tprogressCallback: BiConsumer<Integer,Integer>,\n\tcompletionHandler: Consumer<ExperimentDataStore>\n)
        - cleanOnBackgroundThread(\n\tloader: ExperimentDataLoader,\n\tfilePath: String\n)
        + decompose(\n\tmeasure: Measure,\n\tperiod: int,\n\tprogressCallback: BiConsumer<Integer,Integer>,\n\tcompletionHandler: Consumer<Map<DataType,List<DataPoint>>>\n)
        + emptyCache(file: File)
        + getHeadingComment(): String
        + stopBackgroundThreads()
        + getDataPoints(measure: Measure): List<DataPoint> throws
        + getDataPoints(\n\tmeasure: Measure,\n\toptionalTag: Optional<Tag>\n): List<DataPoint> throws
        + getOmittedRanges(): List<Range<Float>>
        + getOmittedPoints(range: Range<Float>): List<DataPoint>
        + setLoggingEnabled(enabled: boolean)
        + setPreComputingEnabled(enabled: boolean)
    }

    together {

        class ExperimentDataLoader {
            
        }

        class ExperimentDataCleaner {
            
        }

        class ExperimentDataDecomposer {
            
        }

    }

    ExperimentManager <-[dotted]- DataType
    ExperimentManager <-[dotted]- Measure
    ExperimentManager <-[dotted]- Tag
    ExperimentManager <-[dotted]- DataPoint
    ExperimentManager <-[dotted]- Range
    ExperimentManager <-up[dotted]- ExperimentDataLoader
    ExperimentManager <-up[dotted]- ExperimentDataCleaner
    ExperimentManager <-up[dotted]- ExperimentDataDecomposer

}
@enduml

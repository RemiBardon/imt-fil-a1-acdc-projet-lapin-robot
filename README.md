# IMT Atlantique - FIL A1 - Mini-Projet ACDC

*Rémi BARDON*  
*Novembre 2020*

## Présentation

### Thème

Nettoyage, préparation et visualisation de données

### Responsable

Charles Prud’homme – <charles.prudhomme@imt-atlantique.fr>

### Sujet

> Les attentes sociétales en matière d’éthique, de droit et de bien-être animal ont considérablement progressé au cours des dernières années. Les formations en physiologie animale doivent adapter l’offre pédagogique aux attentes des apprenants et à l’évolution de la législation, dans le respect du bien-être animal.
>
> Dans ce contexte, il vous est demandé de participer à la conception d’un simulateur réaliste piloté par de l’intelligence artificielle, se substituant complètement aux animaux vivants pour les travaux pratiques de physiologie expérimentale.
>
> Votre travail consistera à nettoyer, préparer et visualiser les séries temporelles issues des données de physiologie expérimentale collectées au cours des dix dernières années à Oniris. Ce travail devra permettre de préparer le pilotage des fonctions vitales du robot-lapin développé en parallèle.

### Commentaires

> Vous trouverez des informations sur l'extraction de la tendance et de la saisonnalité sur le site suivante :
>
> <https://www.machinelearningplus.com/time-series/time-series-analysis-python/>
>
> En particulier, à partir de la section 6.
>
> Dans notre cas, le modèle additif est tout à fait adapté.
>
> <https://anomaly.io/seasonal-trend-decomposition-in-r/index.html>
>
> <https://en.wikipedia.org/wiki/Decomposition_of_time_series>

## Décisions d'équipe

Les décisions suivantes ont été prises le 8 octobre 2020 dans le but de faire émerger les besoins liés au code métier produit par chacun des 4 étudiants.

Seuls les besoins ont été décidés en commun, l'[API](https://en.wikipedia.org/wiki/API), quant a elle, est le fruit du travail individuel des étudiants.

### Données accessibles

- Donnée brute par `timestamp`, `tag` et mesure
- Tendance par `timestamp`, `tag` et mesure
- Saisonnalité par `timestamp`, `tag` et mesure
- Bruit par `timestamp`, `tag` et mesure
- Étendue (`min` et `max`) de la donnée brute, la tendance, la saisonnalité et le bruit, le tout par `tag` et mesure
- Plages de données omises (début/fin)
- `Tag`s présents dans le fichier
- `Tag`s `"préparation"` et `"euthanasie"` (disponibles, mais pas de convention de nommage)
- Commentaire en haut du fichier (le cas échéant)

### Formattage

- Si une plage de données est retirée, la suite doit être "collée" (éditer les `timestamp`s suivants)

## Décisions personnelles

### Possibilités & restrictions

- 0 ou 1 tag sélectionné à la fois (séance entière ou seulement une partie)
- 1 ou plusieurs mesures sélectionnées à la fois

### Use cases

> "Les 4 graphiques" désigne ici les graphiques temporels (temps en abscisses) de données brutes, tendance, saisonnalité et bruit (en ordonnées).

- Pas de fichier importé
  - Affichage de l'**interface vide**
    - 1 mesure sélectionnée (valeur par défaut)
    - Pas de tag sélectionné
- Fichier importé
  - Affichage de la **pression artérielle** sur les 4 graphiques pour la **séance entière**
    - 1 mesure sélectionnée
    - Pas de tag sélectionné (séance entière)
  - Affichage de la **pression artérielle** sur les 4 graphiques pour la **période de début de la séance**
    - 1 mesure sélectionnée
    - 1 tag sélectionné
  - Affichage de la **fréquence cardiaque** et de la **fréquence respiratoire** sur les 4 graphiques pour la **période de fin de la séance**
    - 2 mesures sélectionnées
    - 1 tag sélectionné

### Architectural Decision Records (ADR)

- Le `timestamp` stocké dans `DataPoint` est un `float` et non un `Float` pour réduire la taille des objets en mémoire.
- La valeur d'un `DataPoint` est un `Float` et non un `float` pour permettre l'utilisation des valeurs `NaN`.
- `DataPoint` ne contient pas de référence à `Tag` pour réduire la taille des objets en mémoire.

### Diagramme de classes

> Remarque: Ceci n'est pas ni diagramme de classes `Java` ni un réel diagramme de classes suivant toutes les normes `UML`. Je l'ai un peu simplifié pour faciliter la lecture.  
> Par exemple, certaines méthodes comme `toString()` ou `hashCode()` ne sont pas présentes.

@startuml
'Désactiver l'affichage en couleur des icônes de visibilité'
skinparam classAttributeIconSize 0

package java {
    package io {
        class File
    }
    package util {
        class List<E>
        class Map<K, V>
        class Optional<T>

        package regex {
            class Pattern
        }
    }
    package text {
        class NumberFormat
    }
}

package com.opencsv {
    class CSVParser
    class CSVReader
}

package code_metier {

    enum Mesure {
        PRESSION_ARTERIELLE
        SPIROMETRIE
        PA_MOYENNE
        FREQUENCE_CARDIAQUE
        FREQUENCE_RESPIRATOIRE
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
        - value: float
        ~ DataPoint(timestamp: float, value: float)
        + getTimestamp(): float
        + getValue(): float
    }

    class ExperimentPhase {
        ~ tag: Tag
        ~ start: float
        ~ end: float
        ~ ExperimentPhase(tag: Tag, start: float, end: float)
        + getTag(): Tag
        + setTag(tag: Tag)
        + getStart(): float
        + setStart(start: float)
        + getEnd(): float
        + setEnd(end: float)
    }
    
    class Range<T> {
        - minimum: T
        - maximum: T
        + Range(minimum: T, maximum: T)
        + getMinimum(): T
        + getMaximum(): T
    }

    class RabitDataLoader {
        - {static} DELIMITER: char
        - {static} PERIOD: int
        - {static} TAG_PREFIX: Pattern
        - {static} FORMAT: NumberFormat

        - headingComment: String
        - phases: Map<Mesure, List<ExperimentPhase>>
        - phasesByTag: Map<Mesure, Map<Tag, ExperimentPhase>>
        - phasesByStart: Map<Mesure, Map<Float, ExperimentPhase>>
        - phasesByEnd: Map<Mesure, Map<Float, ExperimentPhase>>
        - points: Map<DataType, Map<Mesure, List<DataPoint>>>
        - omittedPoints: Map<Mesure, Map<Range<Float>, List<DataPoint>>>

        + RabitDataLoader()

        + reset()
        - {static} addMissingDataTypes(map: Map<DataType, T>, supplier: Supplier<T>)
        - {static} addMissingMeasures(map: Map<DataType, T>, supplier: Supplier<T>)

        + load(file: File) throws
        + cleanData()
        + decomposeData()

        - {static} dataPointsValues(points: List<DataPoint>): double[]

        + getTags(Mesure mesure): List<Tag>
        + getPhases(final Mesure mesure): List<ExperimentPhase>

        + getPoint(type: DataType, mesure: Mesure, timestamp: Float): Optional<DataPoint>
        + getAllPoints(type: DataType, mesures: List<Mesure>, timestamp: Float): Map<Mesure, Optional<DataPoint>>
        + getPoints(type: DataType, mesure: Mesure, tag: Optional<Tag>): List<DataPoint>
        + getAllPoints(type: DataType, mesures: List<Mesure>, tag: Optional<Tag>): Map<Mesure, List<DataPoint>>
        + getPoints(mesure: Mesure, timestamp: Float): Map<DataType, Optional<DataPoint>>
        + getAllPoints(mesures: List<Mesure>, timestamp: Float): Map<DataType, Map<Mesure, Optional<DataPoint>>>
        + getPoints(mesure: Mesure, tag: Optional<Tag>): Map<DataType, List<DataPoint>>
        + getAllPoints(mesures: List<Mesure>, tag: Optional<Tag>): Map<DataType, Map<Mesure, List<DataPoint>>>

        + getValueRange(points: List<DataPoint>): Range<Float>
        + getValueRangeFromMap<T>(points: Map<T, List<DataPoint>>): Map<T, Range<Float>>
        + getValueRangeFromNestedMap<S, T>(points: Map<S, Map<T, List<DataPoint>>>): Map<S, Map<T, Range<Float>>>

        + getOmittedRanges(mesure: Mesure): List<Range<Float>>
        + getAllOmittedRanges(): List<Range<Float>>
        + getOmittedPoints(mesure: Mesure, range: Range<Float>): List<DataPoint>

        + getHeadingComment(): String
    }

    RabitDataLoader <-down[dotted]- DataType
    RabitDataLoader <-down[dotted]- Mesure
    RabitDataLoader <-down[dotted]- Tag
    RabitDataLoader <-down[dotted]- DataPoint
    RabitDataLoader <-down[dotted]- ExperimentPhase
    RabitDataLoader <-down[dotted]- Range

}

code_metier <-down[dotted]- java
code_metier <-down[dotted]- com.opencsv
@enduml

### Structure du projet Java

#### Utilisation de Maven

Pour faciliter l'utilisation de packages permettant la lecture de fichiers [CSV](https://en.wikipedia.org/wiki/Comma-separated_values) (grâce à [opencsv](https://mvnrepository.com/artifact/com.opencsv/opencsv) et la [décomposition de données temporelles](https://en.wikipedia.org/wiki/Decomposition_of_time_series) (avec le package [Seasonal Decomposition of Time Series](https://github.com/ServiceNow/stl-decomp-4j) dans notre cas), j'ai choisi d'utiliser [Maven](https://maven.apache.org/).

#### Nommage des packages

Le nommage des packages a été fait en suivant les [conventions de nommage](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html) décrites par Oracle.

## Prérequis

## Utilisation

```java
final File file = new File(/* ... */);
final RabitDataLoader loader = new RabitDataLoader();

try {
    loader.load(file)
    loader.cleanData()
    loader.decomposeData()
} catch(final IOException e) {
    e.printStackTrace();
} catch(final CsvValidationException e) {
    e.printStackTrace();
} catch(final NumberFormatException e) {
    e.printStackTrace();
}
```

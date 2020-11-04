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

### Diagramme de classes

@startuml
'Désactiver l'affichage en couleur des icônes de visibilité'
skinparam classAttributeIconSize 0

package java.io {
    class File
}

package java.lang {
    class String
    class Range<T>
}

package java.util {
    class Date
    class Optional<T>
    class Dictionary<K, V>
}

package code_metier_lapin_robot {

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
        - value: string
        ~ Tag(value: string)
        + {static} preparation(): Tag
        + {static} euthanasia(): Tag
        + toString(): String
    }

    class DataPoint {
        + timestamp: Date
        + value: float
        ~ DataPoint(timestamp: Date, value: float)
    }

    class CSVScanner {
        - {static} DELIMITER: string
        - headingComment: String
        - start: Date
        - points: Dictionary<DataType, Dictionary<Mesure, DataPoint[]>>
        - omittedPoints: Dictionary<Range<Date>, Dictionary<DataType, Dictionary<Mesure, DataPoint[]>>>
        + CSVScanner()

        + load(file: File)

        + getTags(): Tag[]

        + getStart(): Date
        + getEnd(): Date

        + getPoints(type: DataType, mesure: Mesure, timestamp: Date): DataPoint
        + getPoints(type: DataType, mesures: Mesure[], timestamp: Date): Dictionary<Mesure, DataPoint>
        + getPoints(type: DataType, mesure: Mesure, tag: Optional<Tag>): DataPoint[]
        + getPoints(type: DataType, mesures: Mesure[], tag: Optional<Tag>): Dictionary<Mesure, DataPoint[]>
        + getAllPoints(mesure: Mesure, timestamp: Date): Dictionary<DataType, DataPoint>
        + getAllPoints(mesures: Mesure[], timestamp: Date): Dictionary<DataType, Dictionary<Mesure, DataPoint>>
        + getAllPoints(mesure: Mesure, tag: Optional<Tag>): Dictionary<DataType, DataPoint[]>
        + getAllPoints(mesures: Mesure[], tag: Optional<Tag>): Dictionary<DataType, Dictionary<Mesure, DataPoint[]>>

        + getValueRange(points: DataPoint[]): Range<float>
        + getValueRange<T>(points: Dictionary<T, DataPoint[]>): Dictionary<T, Range<float>>
        + getValueRange<S, T>(points: Dictionary<S, Dictionary<T, DataPoint[]>>): Dictionary<S, Dictionary<T, Range<float>>>

        + getOmittedRanges(): Range<Date>[]
        + getOmittedPoints(range: Range<Date>): Dictionary<DataType, Dictionary<Mesure, DataPoint[]>>

        + getHeadingComment(): String
    }

    CSVScanner <-down[dotted]- DataType
    CSVScanner <-down[dotted]- Mesure
    CSVScanner <-down[dotted]- Tag
    CSVScanner <-down[dotted]- DataPoint

}

code_metier_lapin_robot <-down[dotted]- java.lang
code_metier_lapin_robot <-down[dotted]- java.util
code_metier_lapin_robot <-down[dotted]- java.io
@enduml

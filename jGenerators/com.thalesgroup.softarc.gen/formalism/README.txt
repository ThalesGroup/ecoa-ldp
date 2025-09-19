1. MODELISATION
===============

Le formalisme SOFTARC est un méta-modèle "léger" servant aux différentes étapes de transformation/génération réalisées
par SOFTARC.

Il est modélisé en langage Java, sous la forme de la classe SoftarcFormalismDefinition.

Cette classe contient de nombreuses classes internes, chacune correspondant à une classe du Formalisme.
Les "classes du formalisme" sont les classes internes de la classe SoftarcFormalismDefinition.

Les classes du formalisme sont des classes de pures données ("data objects"), et ont les restrictions suivantes:

- Pas de méthodes, uniquement des champs.
- Chaque champ doit être soit une information élémentaire (attribut), soit un "pointeur" vers une autre classe du Formalisme (relation).

Pour les attributs:

- Les types de base pour les attributs sont les suivants: boolean, long, String.
- Un attribut peut être simple ou multiple (représenté par un tableau dans SoftarcFormalismDefinition). 
Dans ce cas, il doit être du type String.
- Les attributs du formalisme ont toujours une valeur. La valeur par défaut d'un attribut est:
  * false pour le type 'boolean'
  * 0 pour un le type 'long'
  * "" pour un le type 'String'
  * [] pour un attribut multiple

Pour les relations:

- Une relation peut être simple ou multiple. Une relation multiple est représentée par un tableau dans SoftarcFormalismDefinition. Elle est toujours de cardinalité [0..*]. 
- Une relation simple a une cardinalité [0..1]. Par défaut, elle n'a pas de valeur, i.e. 'null'.
- Il y a 2 types de relations: relation d'aggrégation (un objet "contient" un autre), ou référence (un objet "pointe" vers un autre sans le contenir).
- Une relation d'aggrégation est qualifiée avec le mot-clé Java 'protected'; sinon c'est une référence.
- Les relations sont indépendantes les unes des autres. Plusieurs relations "opposées" (ex: référence 'parent'
qui est l'inverse d'une relation d'aggrégation) doivent être maintenues manuellement en cohérence.


En pratique (mais ceci n'est pas modélisé formellement), le formalisme est un graphe d'objets qui sont tous accessibles depuis un objet instance 
unique de la classe System.

Si on considère les relations d'aggrégation uniquement, le graphe est un arbre: i.e. chaque objet est accessible par un unique chemin depuis l'objet 
racine de la classe System.



  

2. GENERATION DE CODE JAVA
==========================


On génére du code Java à partir de la classe de la classe SoftarcFormalismDefinition, 
en exécutant la classe Generate.

Cette exécution peut se faire:

* via Ant, fichier $SOFTARC_HOME/jGenerators/com.thalesgroup.softarc.gen/build.xml, cible "generateFormalism".
* en direct via Eclipse (Run -> Java Application)
* en direct via une ligne de commande du type "java SoftarcFormalismDefinition"

L'exécution n'a pas besoin d'arguments.
La variable d'environnement SOFTARC_HOME doit être définie.
Les templates de génération sont dans le répertoire 
$SOFTARC_HOME/jGenerators/com.thalesgroup.softarc.gen/formalism/src/templates,
et doivent être accessibles dans le classpath Java.

Le code Java généré comprend:

* Des interfaces avec les attributs/relations définis par le formalisme
(classes com.thalesgroup.softarc.sf.*)

* Des classes d'implémentation, nommées "Q*"
(classes com.thalesgroup.softarc.sf.impl.*)


3. EXEMPLE
==========


Pour la classe Assembly

définie dans la classe SoftarcFormalismDefinition 
dans le fichier $SOFTARC_HOME/jGenerators/com.thalesgroup.softarc.gen/formalism/src/formalism/SoftarcFormalismDefinition.java,

on génère:

SOFTARC/jGenerators/com.thalesgroup.softarc.gen/src-gen/com/thalesgroup/softarc/sf/Assembly.java
SOFTARC/jGenerators/com.thalesgroup.softarc.gen/src-gen/com/thalesgroup/softarc/sf/impl/QAssembly.java


4. AUTRES FICHIERS GENERES
==========================


$SOFTARC_HOME/jGenerators/com.thalesgroup.softarc.gen/formalism/attributes.properties

Ce fichier est généré automatiquement à partir du formalisme.
Il contient une description du formalisme sous forme de propriétés.
Cette description est utilisée par l'outil CheckTemplates.


5. EXPORT DU FORMALISME
=======================

Pour la mise au point du générateur, il est possible d'exporter un 'dump' du formalisme au format JSON.

Pour activer cette option, il faut positionner la propriété 'verbose' à la valeur 'true' au niveau Ant:

   ant gen -Ddebug=true

Un fichier JSON est généré après chaque passe de transformation du Formalisme dans GenSoftarc.
Les fichiers sont générés dans 04-Integration/<deployment>/.
Le dernier état du Formalisme (export après la dernière passe) est renommé 'formalism.json'.

Les fichiers JSON générés sont donc les suivants (les noms des passes peuvent changer):

   04-Integration/<deployment>/
      formalism_after_s10.importcomponents.json
      formalism_after_s20.xref.json
      formalism_after_s27.ctypes.json
      formalism_after_s40.importassembly.json
      ...
      formalism.json
   
   
Pour l'exploitation de ces fichiers JSON, il est possible d'utiliser les outils suivants:

jless - https://github.com/PaulJuliusMartinez/jless
(utiliser la version 0.8.0 si la plus récente demande une libc trop récente)

jnv - https://github.com/ynqa/jnv - Interactive JSON filter using jq

jaq - https://github.com/01mf02/jaq - A jq clone focussed on correctness, speed, and simplicity



Exemple d'utilisation

ln 04-Integration/deployment/formalism.json f.json

jaq <requete> f.json | jnv

Exemples de requêtes utilisables dans jq/jaq/jnv

# afficher tous les composants/librairies utilisés:
.components[].types[].xmlID

# la liste des exés:
.mapping.executables[].name

# l'attribut 'xmlID' de tous les objets qui en ont un:
..|objects|select(has("xmlID"))|.xmlID
# ou bien (équivalent):
..|.xmlID?|values

# lister toutes les valeurs du champ 'path':
[..|.path?|values]| unique

# les infos relatives à la production d'un exécutable:
.mapping.executables[0] | (.mainC,.mainCPP,.mainADA,.sources[])

# les infos relatives à la production de tous les exécutables:
.mapping.executables[] | { name,sources:([.mainC,.mainCPP,.mainADA,.mainJAVA]+.sources)}

# tous les threads (mis à plat):
[.mapping.executables[].threads | arrays] | flatten | .[]

# toutes les tailles de pile:
.mapping.executables[].threads  | .. |.stack? 


# lister tous les types par taille décroissante:
[.components[].types[]? | {size,xmlID}] | sort|reverse

# trouver les types plus grands qu'un certain seuil de taille:
.components[].types[]? | {size,xmlID} | select(.size>100) 


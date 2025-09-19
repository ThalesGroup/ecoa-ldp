# ChangeLog MetaModel ECOA AS7

## V1 - 12/09/2023

## V2 - 20/10/2023

* ajout du type uint64 en optionnel
* possibilité d'utiliser un assemblage interne comme assemblage global
* diagrammes UML: identification les attributs requis
* isSynchronous=false par défaut
* ajout Initial_Example AS6 et AS7
* MM+exemple de SystemAssembly [OPTION MULTI APP ASSEMBLY]
* id d'opération optionnel dans le déploiement
* limitation du type Name à 64 caractères
* suppression attributs header/sourceExtension
* suppression inheritanceField
* suppression topic
* support du langage Rust
* PINFO: ajout attribut writable
* ajout de activating et fifoSize sur les data
* ajout de activating sur les implicitLinks
* màj globale des commentaires
* renommage attribut "system" en "kind"
* explication de la syntaxe "$property"
* implem Python: renommage de fullname en packageName

## V3 - 07/12/2023

* corrections suite à relecture DA
* ajout de namespaces XML
* suppression fichier IO.xsd
* ajout start_mode
* suppression de xsd:any
* ajout de library dans Workspace.xsd
* suppression de l'élément /library/types
* renommage TIMERMANAGER=>PERIODIC_TRIGGER_MANAGER
* ajout DYNAMIC_TRIGGER_MANAGER

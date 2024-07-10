
 - [ ] Ajouter des préfixs propres
 - [ ] Gérer ServerName
 - [x] Ajouter un directiveType et enlever le ruleType ?

# Bugs

 - [x] phase en argument passe pas
 - [x] VirtualHost des macros doit être lié aux Uses, pas à la définition
 - [x] pas de VirtualHost ni de location pour la config globale
 - [x] certains param sont bien remplacés et d'autres non
 - [ ] fichier s'affiche pas dans l'output

# Parser

 - [x] Chemins propres en argument
 - [-] SecDefaultAction set la phase par défaut
 - [x] Gérer les:
    - [x] define
    - [x] DefineStr
    - [x] UndefMacro


# Compilation

 - [x] gérer les variables locales + les paramètres
 - [ ] gérer les:
    - [x] IfDefine
    - [x] IfModule
    - [ ] LocationMatch
 - [-] évaluer les conditions pour de vrai

 --------------------------------
 - [x] placer le modele dans le context ?
 - [ ] optimize by replacing regex with automn ?
 - [ ] ce serait cool que les variables dans les conditions fassent partie de l'Ontologie
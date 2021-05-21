# JAVACARD-Acos3

The project contains two files :

JC_ACOS3.java : contains the code correspending to the applet

jc_acos3.scr : contains a script that describes a specific scenario for the use of the developed applet. Mainly compesed of APDU commands. The results will be visible in the output window. This is alowing us to test our applet and check the consistency of the APDU responses for each APDU command



1. Objectif :


• Créer un applet permettant le même fonctionnement de la carte ACOS3
respectant la norme ISO7816.

• Créer un script de test permettant la création d’un fichier utilisateur
avec un FID : A0A0 contenant 4 enregistrements de 32 octets.


2. Les étapes du projet‘ACOS3Sim’:


 -Instructions supportées par l’applet :
 
 
• 0xA4 : Select file (Le code administrateur doit être vérifié)

• 0x20 : soumettre un code (administrateur ou PIN)

• 0x30 : réinitialiser l’applet (Le code administrateur doit être vérifié)

• 0xD2 : Ecrire un enregistrement (selon les attributs de sécurité)

• 0xB2 : Lecture d’un enregistrement (selon les attributs de sécurité)

• 0x24 : Changement du code PIN (Le code PIN initial doit être vérifié)


 -Caractéristiques des fichiers internes :

![FF](https://user-images.githubusercontent.com/76595864/119158604-ead0e300-ba45-11eb-8a40-0d36d7a57f8e.jpg)


3- Commandes APDU 

![APDU](https://user-images.githubusercontent.com/76595864/119158803-2075cc00-ba46-11eb-9f60-87b23e632422.jpg)


4- La création d’un fichier utilisateur avec un FID A0A0 :

Taille : 4 enregistrements de 32 octet

# MPC Fill XML Generator

This project is used to create the cards.xml file that the MPC AutoFill process from MPCFill needs.

## Getting started

### Requires
- Java 11 JRE

To run the generator, get the jar from the releases page or download the source and follow the compile instructions below.

To run the jar, get the folder of image files you wish to construct the xml for. I will refer to this folder as `[CARDS]` for this guide.

If you need cards to have multiple printings, you will need to create a config file. You can refer to the `example.conf` included on this page.

To generate the xml, run the following command:

    java -jar MPCFillXmlGenerator.jar -cards [CARDS] -cfg example.conf -out ./

If you wish to put the cards.xml file directory inside the directory, replace the `./` with `[CARDS]`

## Additional Information

Cards downloaded from CardConjourer dont fit the MPC size requirements perfectly, so this process attempts to increase their size such that the frame will fit perfectly within the bounds of MPC creation.

If any cards have images for their backs that are not defined in `CARD_BACK` for the config, you will need to tweak the created xml file a bit to have MPCFill pick it up.

1. Find the card back image entry in the xml, cut it out and paste it into the `<backs>` entry at the bottom. Note the slot of this card.

2. Find the front facing card and copy the slot number from the front into the back entry you just cut. 

3. Change the slot in the card with the largest slot value in `<fronts>` to be the slot you noted in Step 1.

4. Decrease the `<quantity>` field at the top of the file to reflect the new card total.

5. Repeat for additional card backs.

## Compiling
### Requires:
- Apache Ant
- Java 11 (or greater)

Run

    ant

In the base directory to compile the jar. It can be found in `build/jar` to be copied where needed.
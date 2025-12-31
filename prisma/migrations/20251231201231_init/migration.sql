/*
  Warnings:

  - You are about to drop the column `playerOneDefensePrompt` on the `Game` table. All the data in the column will be lost.
  - You are about to drop the column `playerTwoDefensePrompt` on the `Game` table. All the data in the column will be lost.

*/
-- AlterTable
ALTER TABLE "Game" DROP COLUMN "playerOneDefensePrompt",
DROP COLUMN "playerTwoDefensePrompt";

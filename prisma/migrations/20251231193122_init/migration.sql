/*
  Warnings:

  - You are about to drop the column `isVerified` on the `User` table. All the data in the column will be lost.
  - You are about to drop the column `resetToken` on the `User` table. All the data in the column will be lost.
  - You are about to drop the column `resetTokenExpiry` on the `User` table. All the data in the column will be lost.
  - You are about to drop the column `avatarUrl` on the `UserProfile` table. All the data in the column will be lost.
  - You are about to drop the column `bio` on the `UserProfile` table. All the data in the column will be lost.
  - You are about to drop the `AIGenerationLog` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `CardProgress` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `Deck` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `Flashcard` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `StudyStats` table. If the table is not empty, all the data it contains will be lost.

*/
-- CreateEnum
CREATE TYPE "GameStatus" AS ENUM ('WAITING_FOR_PLAYER', 'DEFENSE_PHASE', 'ATTACK_PHASE', 'COMPLETED', 'ABANDONED');

-- CreateEnum
CREATE TYPE "GamePhase" AS ENUM ('DEFENSE', 'ATTACK');

-- CreateEnum
CREATE TYPE "GameEndReason" AS ENUM ('HP_DEPLETED', 'FULL_CONVICTION', 'CREDITS_EXHAUSTED', 'PLAYER_FORFEIT', 'TIMEOUT');

-- DropForeignKey
ALTER TABLE "CardProgress" DROP CONSTRAINT "CardProgress_flashcardId_fkey";

-- DropForeignKey
ALTER TABLE "Deck" DROP CONSTRAINT "Deck_profileId_fkey";

-- DropForeignKey
ALTER TABLE "Flashcard" DROP CONSTRAINT "Flashcard_deckId_fkey";

-- DropForeignKey
ALTER TABLE "StudyStats" DROP CONSTRAINT "StudyStats_profileId_fkey";

-- AlterTable
ALTER TABLE "RefreshToken" ADD COLUMN     "ipAddress" TEXT,
ADD COLUMN     "userAgent" TEXT;

-- AlterTable
ALTER TABLE "User" DROP COLUMN "isVerified",
DROP COLUMN "resetToken",
DROP COLUMN "resetTokenExpiry",
ADD COLUMN     "isEmailVerified" BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN     "verificationExpiry" TIMESTAMP(3);

-- AlterTable
ALTER TABLE "UserProfile" DROP COLUMN "avatarUrl",
DROP COLUMN "bio",
ADD COLUMN     "dailyGamesPlayed" INTEGER NOT NULL DEFAULT 0,
ADD COLUMN     "draws" INTEGER NOT NULL DEFAULT 0,
ADD COLUMN     "gamesPlayed" INTEGER NOT NULL DEFAULT 0,
ADD COLUMN     "lastGameDate" TIMESTAMP(3),
ADD COLUMN     "losses" INTEGER NOT NULL DEFAULT 0,
ADD COLUMN     "wins" INTEGER NOT NULL DEFAULT 0,
ALTER COLUMN "displayName" DROP NOT NULL;

-- DropTable
DROP TABLE "AIGenerationLog";

-- DropTable
DROP TABLE "CardProgress";

-- DropTable
DROP TABLE "Deck";

-- DropTable
DROP TABLE "Flashcard";

-- DropTable
DROP TABLE "StudyStats";

-- CreateTable
CREATE TABLE "ScenarioTemplate" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "characterTemplate" TEXT NOT NULL,
    "secretTemplate" TEXT NOT NULL,
    "variables" JSONB NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "ScenarioTemplate_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Game" (
    "id" TEXT NOT NULL,
    "playerOneId" TEXT NOT NULL,
    "playerTwoId" TEXT,
    "templateId" TEXT NOT NULL,
    "generatedCharacter" TEXT NOT NULL,
    "generatedSecret" TEXT NOT NULL,
    "playerOneDefensePrompt" TEXT,
    "playerTwoDefensePrompt" TEXT,
    "playerOneDefenseSummary" TEXT,
    "playerTwoDefenseSummary" TEXT,
    "status" "GameStatus" NOT NULL DEFAULT 'WAITING_FOR_PLAYER',
    "phase" "GamePhase" NOT NULL DEFAULT 'DEFENSE',
    "playerOneAiHp" INTEGER NOT NULL DEFAULT 100,
    "playerTwoAiHp" INTEGER NOT NULL DEFAULT 100,
    "maxCharsPerMessage" INTEGER NOT NULL DEFAULT 250,
    "maxTurnsPerPhase" INTEGER NOT NULL DEFAULT 5,
    "winnerId" TEXT,
    "endReason" "GameEndReason",
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Game_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "GameTurn" (
    "id" TEXT NOT NULL,
    "gameId" TEXT NOT NULL,
    "playerId" TEXT NOT NULL,
    "phase" "GamePhase" NOT NULL,
    "turnNumber" INTEGER NOT NULL,
    "playerMessage" TEXT NOT NULL,
    "aiResponse" TEXT NOT NULL,
    "convictionScore" INTEGER,
    "hpDamage" INTEGER,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "GameTurn_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "MatchmakingQueue" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "joinedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "MatchmakingQueue_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "ScenarioTemplate_name_key" ON "ScenarioTemplate"("name");

-- CreateIndex
CREATE UNIQUE INDEX "MatchmakingQueue_userId_key" ON "MatchmakingQueue"("userId");

-- AddForeignKey
ALTER TABLE "Game" ADD CONSTRAINT "Game_playerOneId_fkey" FOREIGN KEY ("playerOneId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Game" ADD CONSTRAINT "Game_playerTwoId_fkey" FOREIGN KEY ("playerTwoId") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Game" ADD CONSTRAINT "Game_templateId_fkey" FOREIGN KEY ("templateId") REFERENCES "ScenarioTemplate"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "GameTurn" ADD CONSTRAINT "GameTurn_gameId_fkey" FOREIGN KEY ("gameId") REFERENCES "Game"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "GameTurn" ADD CONSTRAINT "GameTurn_playerId_fkey" FOREIGN KEY ("playerId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

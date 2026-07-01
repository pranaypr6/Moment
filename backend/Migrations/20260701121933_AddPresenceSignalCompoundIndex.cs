using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Moment.Api.Migrations
{
    /// <inheritdoc />
    public partial class AddPresenceSignalCompoundIndex : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_PresenceSignals_SenderUserId",
                table: "PresenceSignals");

            migrationBuilder.CreateIndex(
                name: "IX_PresenceSignals_SenderUserId_RelationshipId_Type_CreatedAtU~",
                table: "PresenceSignals",
                columns: new[] { "SenderUserId", "RelationshipId", "Type", "CreatedAtUtc" });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_PresenceSignals_SenderUserId_RelationshipId_Type_CreatedAtU~",
                table: "PresenceSignals");

            migrationBuilder.CreateIndex(
                name: "IX_PresenceSignals_SenderUserId",
                table: "PresenceSignals",
                column: "SenderUserId");
        }
    }
}

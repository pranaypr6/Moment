using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Moment.Api.Migrations
{
    /// <inheritdoc />
    public partial class AddPerformanceIndexes : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateIndex(
                name: "IX_Users_RefreshToken",
                table: "Users",
                column: "RefreshToken");

            migrationBuilder.CreateIndex(
                name: "IX_Users_PreviousRefreshToken",
                table: "Users",
                column: "PreviousRefreshToken");

            migrationBuilder.CreateIndex(
                name: "IX_Invites_SenderUserId_CreatedAt",
                table: "Invites",
                columns: new[] { "SenderUserId", "CreatedAt" });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_Users_RefreshToken",
                table: "Users");

            migrationBuilder.DropIndex(
                name: "IX_Users_PreviousRefreshToken",
                table: "Users");

            migrationBuilder.DropIndex(
                name: "IX_Invites_SenderUserId_CreatedAt",
                table: "Invites");
        }
    }
}
